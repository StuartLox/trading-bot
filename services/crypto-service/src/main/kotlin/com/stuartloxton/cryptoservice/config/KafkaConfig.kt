package com.stuartloxton.cryptoservice.config


import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.StreamsConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.core.*
import kotlin.collections.HashMap


@Configuration
@EnableKafka
@ConfigurationProperties(prefix = "config")
class KafkaConfig {
    private val log = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Value("\${application.kafka.quote-event-topic}")
    var quotesTopic = ""

    @Value("\${application.kafka.crypto-metrics-topic}")
    var cryptoMetricsTopic = ""

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

    fun getStreamsConfig(): HashMap<String, Any> {
        val config = HashMap<String, Any>()
        config.putAll(commonConfig())
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, streamsBtcMetricsGroupId)
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        return config
    }

    @Bean("streamBuilder")
    fun streamBuilderFactoryBean(): StreamsBuilderFactoryBean {
        val config = getStreamsConfig()
        return StreamsBuilderFactoryBean(KafkaStreamsConfiguration(config), CleanupConfig(true,true))
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
//    fun avgPriceConsumer(): ConsumerFactory<BitcoinMetricEventWindow, BitcoinMetricEventWindow> {
//        val schemaRegistryClient: SchemaRegistryClient = CachedSchemaRegistryClient(schemaRegistryUrl, 3)
//        val btcMetricEventWindow = SpecificAvroSerde<BitcoinMetricEventWindow>(schemaRegistryClient)
//        val btcMetricEventWindow = SpecificAvroSerde<BitcoinMetricEvent>(schemaRegistryClient)
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
//
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