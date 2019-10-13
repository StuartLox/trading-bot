package streams

import models.Stock
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service


@Service
class StockEventProducer(private val kafkaTemplate: KafkaTemplate<String, Stock>){

    private val log = LoggerFactory.getLogger(StockEventProducer::class.java)
    private val kafkaProducerTopic: String = "bitcoin-price-aud.v1"

    fun stockEventProducer(stock: Stock): Boolean {
        log.info("Start of $kafkaProducerTopic || Modify Address Event")
        log.debug("Input Data: $stock")
        var success = false
        try {
            kafkaTemplate.send(kafkaProducerTopic, stock)
            success = true
        } catch (e: Exception) {
            log.error("Exception in CustomerEvent.||CustomerEventProducer $e")
            success = false
        } finally {
            log.debug("Stock Event : Status $success")
            return success
        }
    }
}