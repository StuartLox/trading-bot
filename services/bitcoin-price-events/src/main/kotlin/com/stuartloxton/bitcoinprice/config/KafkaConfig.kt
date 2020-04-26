package com.stuartloxton.bitcoinprice.config

//import com.stuartloxton.bitcoinprice.AveragePrice
//import com.stuartloxton.bitcoinprice
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
    var btcEventTopic = ""

    @Value("\${application.kafka.btc-metrics-topic}")
    var btcMetricsTopic = ""

    @Value("\${application.kafka.bootstrap}")
    var bootstrapUrl: String = ""

    @Value("\${application.kafka.schema-registry}")
    var schemaRegistryUrl: String = ""

    @Value("\${application.kafka.group-id}")
    var groupId: String = ""

    @Value("\${application.kafka.avg-btc-group-id}")
    var streamsBtcMetricsGroupId: String = ""

    @Value("\${application.kafka.hasSecret}")
    var hasSecret: Boolean = false

    @Value("\${application.kafka.username}")
    var username: String = ""

    @Value("\${application.kafka.password}")
    var password: String = ""

    private fun commonConfig(): HashMap<String, Any> {
        val config = HashMap<String, Any>()
        config.put("bootstrap.servers", bootstrapUrl)
        config.put("schema.registry.url", schemaRegistryUrl)
        // Add
        if (hasSecret) addSecret(config)
        return config
    }

    private fun addSecret(config: HashMap<String, Any>): HashMap<String, Any> {
        val clsPath = "org.apache.kafka.common.security.plain.PlainLoginModule"
        val jaasCfg = "$clsPath required username=\"$username\" password=\"$password\";".format(username, password)
        config.put("sasl.jaas.config", jaasCfg)
        config.put("security.protocol", "SASL_PLAINTEXT")
        config.put("sasl.mechanism", "PLAIN")
        return config
    }

    private fun getStreamsConfig(): HashMap<String, Any> {
        val config = HashMap<String, Any>()
        config.putAll(commonConfig())
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, streamsBtcMetricsGroupId)
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        config.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, StockTimestampExtractor::class.java)
        return config
    }

    @Bean("streamBuilder")
    fun streamBuilderFactoryBean(): StreamsBuilderFactoryBean {
        val config = getStreamsConfig()
        val factory = StreamsBuilderFactoryBean(KafkaStreamsConfiguration(config), CleanupConfig(true,true))
        return factory
    }

    private fun consumerConfig(): HashMap<String, Any> {
        val config = HashMap<String, Any>()
        config.putAll(commonConfig())
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        config.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        return config
    }

//    @Bean
//    fun avgPriceConsumer(): ConsumerFactory<, AveragePrice> {
//        val schemaRegistryClient: SchemaRegistryClient = CachedSchemaRegistryClient(schemaRegistryUrl, 3)
//        val avgPriceSpecificAvroSerde = SpecificAvroSerde<AveragePrice>(schemaRegistryClient)
//        val avgPriceWindowSpecificAvroSerde = SpecificAvroSerde<AveragePriceWindow>(schemaRegistryClient)
//
//    val defaultSerdeConfig = Collections.singletonMap(
//        KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
//        schemaRegistryUrl)
//
//        avgPriceSpecificAvroSerde.configure(defaultSerdeConfig,false)
//        avgPriceWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)
//
//        return DefaultKafkaConsumerFactory(
//            consumerConfig(), avgPriceWindowSpecificAvroSerde.deserializer(),
//            avgPriceSpecificAvroSerde.deserializer()
//        )
//    }

//    @Bean
//    fun kafkaListenerContainerFactory(): KafkaListenerContainerFactory<*>? {
//        val factory =
//            ConcurrentKafkaListenerContainerFactory<AveragePriceWindow, AveragePrice>()
//        factory.consumerFactory = avgPriceConsumer()
//        factory.containerProperties.ackMode =
//            ContainerProperties.AckMode.MANUAL
//        factory.isBatchListener = true
//        return factory
//    }
}