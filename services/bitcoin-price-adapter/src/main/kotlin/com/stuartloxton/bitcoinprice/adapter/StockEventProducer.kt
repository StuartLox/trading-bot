package com.stuartloxton.bitcoinprice.adapter

import com.stuartloxton.bitcoinprice.adapter.config.KafkaConfig
import com.stuartloxton.bitcoinpriceadapter.Stock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service


@Service
class StockEventProducer(private val kafkaTemplate: KafkaTemplate<String, Stock>){

    @Autowired
    private lateinit var kafkaConfig: KafkaConfig

    private val log = LoggerFactory.getLogger(StockEventProducer::class.java)

    private fun publishEvent(topic: String, stock: Stock) {
        log.info(stock.toString())
        kafkaTemplate.send(topic, stock.getSymbol(), stock)
    }

    fun stockEventProducer(stocks: List<Stock>): Boolean {
        val kafkaProducerTopic: String = kafkaConfig.btc_event_topic
        log.info("Start of $kafkaProducerTopic || Stock Event")

        var success = false

        try {
            stocks.forEach{ publishEvent(kafkaProducerTopic, it) }
            success = true
        } catch (e: Exception) {
            log.error("Exception in StockEvent.||StockEventProducer $e")
        } finally {
            log.info("Stock Event : Status $success")
            return success
        }
    }
}