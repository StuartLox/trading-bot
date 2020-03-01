package com.stuartloxton.bitcoinprice.adapter.config

import com.stuartloxton.bitcoinpriceadapter.Stock
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory


@Configuration
@EnableKafka
@ConfigurationProperties(prefix = "config")
class KafkaConfig {
    private val log = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Value("\${application.kafka.btc-event-topic}")
    var btc_event_topic = ""

    @Value("\${application.kafka.bootstrap}")
    var bootstrapUrl: String = ""

    @Value("\${application.kafka.schema-registry}")
    var schemaRegistryUrl: String = ""

    @Bean
    fun producerConfig(): HashMap<String, Any> {
        val producerProps = HashMap<String, Any>()
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,  KafkaAvroSerializer::class.java)
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapUrl)
        producerProps.put("schema.registry.url", schemaRegistryUrl)
        val jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";"
        val jaasCfg = String.format(jaasTemplate, "test", "test123")
        producerProps.put("schema.registry.url", schemaRegistryUrl)
        producerProps.put("security.protocol", "SASL_PLAINTEXT")
        producerProps.put("sasl.mechanism", "PLAIN")
        producerProps.put("sasl.jaas.config", jaasCfg)

        return producerProps
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, Stock> {
        return DefaultKafkaProducerFactory(producerConfig())
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Stock> {
        return KafkaTemplate(producerFactory())
    }
}