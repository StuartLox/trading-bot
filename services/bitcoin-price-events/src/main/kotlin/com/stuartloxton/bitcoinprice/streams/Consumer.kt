package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.AveragePrice
import com.stuartloxton.bitcoinprice.AveragePriceWindow
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment


class Consumer {
    private val logger = LoggerFactory.getLogger(javaClass)
    var avePriceTable: MutableMap<Any, Any> = mutableMapOf()
    @KafkaListener(
        topics = ["avg-bitcoin-price-topic.v1"],
        groupId = "avg-bitcoin-price-consumer.v1.0.1",
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

    fun processEvent(key: Any, value:Any): Boolean {
        logger.info("Key - Average Price Window: $key")
        logger.info("Value - Average Price: $value")
        logger.info("\n\n\n\nTESTING")
        avePriceTable.putIfAbsent(key,value)
        return true
    }
}