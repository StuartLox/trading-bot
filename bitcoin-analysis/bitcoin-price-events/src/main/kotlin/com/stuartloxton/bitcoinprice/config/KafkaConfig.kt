package com.stuartloxton.bitcoinprice.config

import com.stuartloxton.bitcoinprice.Stock
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.streams.StreamsConfig
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory


@Configuration
@EnableKafka
class KafkaConfig {
    private val log = LoggerFactory.getLogger(KafkaConfig::class.java)


    private val bootstrapServerUrl: String = "localhost:9092"
    private val schemaUrl: String = "http://localhost:8081"


    @Bean
    fun producerConfig(): HashMap<String, Any> {
        val producerProps = HashMap<String, Any>()
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] =
            KafkaAvroSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
            KafkaAvroSerializer::class.java
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServerUrl
        producerProps["schema.registry.url"] = schemaUrl
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

    @Bean(name = arrayOf(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME))
    fun kStreamsConfigs(): StreamsConfig {
        val props = HashMap<String, Any>()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "test-streams"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServerUrl
        return StreamsConfig(props)
    }
}