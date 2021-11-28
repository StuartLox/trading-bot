package com.stuartloxton.binance.gateway

import com.stuartloxton.binance.gateway.config.KafkaConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service


@Service
class CryptoQuoteProcessor(private val kafkaTemplate: KafkaTemplate<String, Quote>){

    @Autowired
    private lateinit var kafkaConfig: KafkaConfig

    private val log = LoggerFactory.getLogger(CryptoQuoteProcessor::class.java)

    fun cryptoQuoteProcessor(quote: Quote?) {
        val kafkaProducerTopic: String = kafkaConfig.cryptoPriceTopic
        if (quote != null) {
            log.info("Binance Quotes || $quote")
            kafkaTemplate.send(kafkaProducerTopic, quote.getSymbol(), quote)
        }
    }
}