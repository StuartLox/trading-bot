package com.stuartloxton.bitcoinprice.streams


import com.stuartloxton.bitcoinprice.Stock
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service


@Service
class StockEventProducer(private val kafkaTemplate: KafkaTemplate<String, Stock>){

    private val log = LoggerFactory.getLogger(StockEventProducer::class.java)
    private val kafkaProducerTopic: String = "bitcoin-price-aud.v8"

    fun stockEventProducer(stock: Stock): Boolean {
        log.info("Start of $kafkaProducerTopic || Stock Event")
        log.debug("Input Data: $stock")
        var success = false
        try {
            log.info(stock.toString())
            kafkaTemplate.send(kafkaProducerTopic, stock.getSymbol(), stock)
            success = true
        } catch (e: Exception) {
            log.error("Exception in StockEvent.||StockEventProducer $e")
            success = false
        } finally {
            log.info("Stock Event : Status $success")
            return success
        }
    }
}