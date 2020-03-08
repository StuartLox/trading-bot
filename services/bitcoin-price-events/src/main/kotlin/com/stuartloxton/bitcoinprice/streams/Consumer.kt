package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.AveragePrice
import com.stuartloxton.bitcoinprice.AveragePriceWindow
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Value("\${application.kafka.avg-price-topic}")
const val topic: String = ""

@Autowired
private lateinit var inference: Inference

@Service
class Consumer {
    private val logger = LoggerFactory.getLogger(javaClass)
    var avePriceItems = mutableListOf<List<Double>>()
    @KafkaListener(
        topics = ["avg-bitcoin-price.v1"],
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

    fun processEvent(key: AveragePriceWindow, value:AveragePrice): Boolean {
        logger.info("Key - Average Price Window: $key")
        logger.info("Value - Average Prices: $value")
        avePriceItems.add(listOf(value.getAveragePrice(), value.getAveragePrice()))
        val prediction = inference.getPrediction(avePriceItems)
        logger.info("Inference: $prediction, Items: ${avePriceItems.size}")
        return true
    }
}