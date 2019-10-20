package com.stuartloxton.bitcoinprice.config

import com.stuartloxton.bitcoinprice.Stock
import com.stuartloxton.bitcoinprice.serdes.StockSerde
import com.stuartloxton.bitcoinprice.serdes.StockTimestampExtractor
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.core.CleanupConfig
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

//    @Bean(name = arrayOf(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME))
//    fun kStreamsConfigs(): StreamsConfig {
//        val config = HashMap<String, Any>()
//        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "default")
//        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerUrl)
//        return StreamsConfig(config)
//    }

//    fun setDefaults(config: HashMap<String, Any>): HashMap<String, Any> {
//        val props = HashMap<String, Any>()
//        props[StreamsConfig.APPLICATION_ID_CONFIG] = "test-streams.v2"
//        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServerUrl
//        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
//        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = "io.confluent.kafka.serializers.KafkaAvroDeserializer"
//        return props
//    }

    @Bean("app1StreamBuilder")
    fun app1StreamBuilderFactoryBean(): StreamsBuilderFactoryBean {
        val config = HashMap<String, Any>()
        val stock =  StockSerde()
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "App.v1")
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerUrl)
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().javaClass)
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde::class.java)
        config.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, StockTimestampExtractor::class.java)
        config.put("schema.registry.url", "http://localhost:8081")
        val factory = StreamsBuilderFactoryBean(KafkaStreamsConfiguration(config), CleanupConfig(true,true))
        return factory
    }
}