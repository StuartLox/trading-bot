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

    @Value("\${application.kafka.hasSecret}")
    var hasSecret: Boolean = false

    @Value("\${application.kafka.username}")
    var username: String = ""

    @Value("\${application.kafka.password}")
    var password: String = ""

    fun commonConfig(): HashMap<String, Any> {
        val config = HashMap<String, Any>()
        config.put("bootstrap.servers", bootstrapUrl)
        config.put("schema.registry.url", schemaRegistryUrl)
        if (hasSecret) {
            val jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";"
            val jaasCfg = String.format(jaasTemplate, username, password)
            config.put("sasl.jaas.config", jaasCfg)
            config.put("security.protocol", "SASL_PLAINTEXT")
            config.put("sasl.mechanism", "PLAIN")
        }
        return config
    }
    @Bean
    fun producerConfig(): HashMap<String, Any> {
        val props = HashMap<String, Any>()
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,  KafkaAvroSerializer::class.java)
        props.putAll(commonConfig())
        return props
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