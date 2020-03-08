package com.stuartloxton.bitcoinprice.config

import com.stuartloxton.bitcoinprice.AveragePrice
import com.stuartloxton.bitcoinprice.AveragePriceWindow
import com.stuartloxton.bitcoinpriceadapter.Stock
import com.stuartloxton.bitcoinprice.streams.StockTimestampExtractor
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.streams.StreamsConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import java.util.*
import kotlin.collections.HashMap


@Configuration
@EnableKafka
@ConfigurationProperties(prefix = "config")
class KafkaConfig {
    private val log = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Value("\${application.kafka.btc-event-topic}")
    var btc_event_topic = ""

    @Value("\${application.kafka.avg-price-topic}")
    var avg_price_topic = ""

    @Value("\${application.kafka.bootstrap}")
    var bootstrapUrl: String = ""

    @Value("\${application.kafka.schema-registry}")
    var schemaRegistryUrl: String = ""

    @Value("\${application.kafka.group-id}")
    var groupId: String = ""

    @Value("\${application.kafka.avg-btc-group-id}")
    var streamGroupId: String = ""
//
//    @Bean
//    fun producerConfig(): HashMap<String, Any> {
//        val producerProps = HashMap<String, Any>()
//        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
//        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,  KafkaAvroSerializer::class.java)
//        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapUrl)
//        producerProps.put("schema.registry.url", schemaRegistryUrl)
//        val jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";"
//        val jaasCfg = String.format(jaasTemplate, "test", "test123")
//        producerProps.put("sasl.jaas.config", jaasCfg)
//        return producerProps
//    }
//
//    @Bean
//    fun producerFactory(): ProducerFactory<String, Stock> {
//        return DefaultKafkaProducerFactory(producerConfig())
//    }
//
//    @Bean
//    fun kafkaTemplate(): KafkaTemplate<String, Stock> {
//        return KafkaTemplate(producerFactory())
//    }

    fun getStreamsConfig(): HashMap<String, Any> {
        val config = HashMap<String, Any>()
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, groupId)
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapUrl)
        config.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, StockTimestampExtractor::class.java)
        config.put("schema.registry.url", schemaRegistryUrl)
        val jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";"
        val jaasCfg = String.format(jaasTemplate, "test", "test123")
        config.put("sasl.jaas.config", jaasCfg)
        config.put("security.protocol", "SASL_PLAINTEXT")
        config.put("sasl.mechanism", "PLAIN")
        return config
    }

    @Bean("app1StreamBuilder")
    fun app1StreamBuilderFactoryBean(): StreamsBuilderFactoryBean {
        val config = getStreamsConfig()
        val factory = StreamsBuilderFactoryBean(KafkaStreamsConfiguration(config), CleanupConfig(true,true))
        return factory
    }

    fun consumerConfig(): HashMap<String, Any> {
        val consumerFactoryProperties = HashMap<String, Any>()
        consumerFactoryProperties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapUrl
        consumerFactoryProperties[ConsumerConfig.GROUP_ID_CONFIG] = streamGroupId
        consumerFactoryProperties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        consumerFactoryProperties[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] =
            true
        consumerFactoryProperties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        consumerFactoryProperties["schema.registry.url"] = schemaRegistryUrl
        val jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";"
        val jaasCfg = String.format(jaasTemplate, "test", "test123")
        consumerFactoryProperties["sasl.jaas.config"] = jaasCfg
        consumerFactoryProperties["security.protocol"] = "SASL_PLAINTEXT"
        consumerFactoryProperties["sasl.mechanism"] = "PLAIN"
        return consumerFactoryProperties
    }

    @Bean
    fun avgPriceConsumer(): ConsumerFactory<AveragePriceWindow, AveragePrice> {
        val schemaRegistryClient: SchemaRegistryClient = CachedSchemaRegistryClient(schemaRegistryUrl, 3)
        val avgPriceSpecificAvroSerde = SpecificAvroSerde<AveragePrice>(schemaRegistryClient)
        val avgPriceWindowSpecificAvroSerde = SpecificAvroSerde<AveragePriceWindow>(schemaRegistryClient)

    val defaultSerdeConfig = Collections.singletonMap(
        KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
        schemaRegistryUrl)

        avgPriceSpecificAvroSerde.configure(defaultSerdeConfig,false)
        avgPriceWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)

        return DefaultKafkaConsumerFactory(
            consumerConfig(), avgPriceWindowSpecificAvroSerde.deserializer(),
            avgPriceSpecificAvroSerde.deserializer()
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(): KafkaListenerContainerFactory<*>? {
        val factory =
            ConcurrentKafkaListenerContainerFactory<AveragePriceWindow, AveragePrice>()
        factory.consumerFactory = avgPriceConsumer()
        factory.containerProperties.ackMode =
            ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.isBatchListener = true
        return factory
    }
}