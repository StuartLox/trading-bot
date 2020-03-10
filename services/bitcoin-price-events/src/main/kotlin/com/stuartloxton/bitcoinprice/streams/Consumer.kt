package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.AveragePrice
import com.stuartloxton.bitcoinprice.AveragePriceWindow
import com.stuartloxton.bitcoinprice.config.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service


@Service
class Consumer {
    private val logger = LoggerFactory.getLogger(javaClass)
    var avePriceItems = mutableListOf<List<Double>>()

    @Autowired
    private lateinit var inference: Inference

    @KafkaListener(
        topics = ["aggregated.avg-bitcoin-price.v1"],
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(
        averagePrices: List<ConsumerRecord<AveragePriceWindow, AveragePrice>>,
        ack: Acknowledgment
    ) {
        var commitOffsets = false

        try {
             averagePrices.forEach{
                if (it.key() != null) {
                    val key = it.key()
                    val value = it.value()
                    commitOffsets = processEvent(key, value)
                }
            }

        } catch (e: Exception) {
            commitOffsets = false
        } finally {
            if (commitOffsets) {
                ack.acknowledge()
            }
        }
    }

    fun processEvent(key: AveragePriceWindow, value: AveragePrice): Boolean {
        avePriceItems.add(listOf(value.getAveragePrice(), value.getAveragePrice()))
        val prediction = inference.getPrediction(avePriceItems)
        val datetime = DateTime(key.getWindowEnd()).toLocalDateTime()
        logger.info("Inference: $prediction, Items: ${avePriceItems.size}, windowEnd: $datetime")
        if (prediction == -1.0) {
            return false
        }
        else {
            avePriceItems.removeAt(0)
            logger.info("Removing first elem of in memory db: ${avePriceItems.size}")
            return true
        }
    }
}