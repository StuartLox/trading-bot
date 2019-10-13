package config

import io.confluent.kafka.serializers.KafkaAvroSerializer
import models.Stock
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
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

        val producerProps = HashMap<String, Any>()  //doubt
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

}