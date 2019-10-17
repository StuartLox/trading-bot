package com.stuartloxton.bitcoinprice.streams


import com.stuartloxton.bitcoinprice.models.Stock
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service


@Service
class StockEventProducer(private val kafkaTemplate: KafkaTemplate<String, String>){

    private val log = LoggerFactory.getLogger(StockEventProducer::class.java)
    private val kafkaProducerTopic: String = "bitcoin-price-aud.v3"

    fun stockEventProducer(stock: Stock): Boolean {
        log.info("Start of $kafkaProducerTopic || Stock Event")
        log.debug("Input Data: $stock")
        var success = false
        try {
            log.info(stock.toString())
            val s = Stock("BTC-AUD",1571180400,12.0,13.1,54.3,12.23, 92.2 )
            kafkaTemplate.send(kafkaProducerTopic, s.symbol, s.symbol)
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