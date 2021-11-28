package com.stuartloxton.cryptoservice.streams

import com.stuartloxton.binance.gateway.Quote
import com.stuartloxton.cryptoservice.CryptoMetricEvent
import com.stuartloxton.cryptoservice.CryptoMetricEventWindow
import com.stuartloxton.cryptoservice.config.KafkaConfig
import com.stuartloxton.cryptoservice.streams.metrics.CryptoMetric
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.kstream.Materialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*


@Component
class Streams {
    @Autowired
    private lateinit var cryptoMetric: CryptoMetric

    @Bean("kafkaStreamProcessing")
    fun startProcessing(@Qualifier("streamBuilder") builder: StreamsBuilder, kafkaConfig: KafkaConfig): KStream<CryptoMetricEventWindow, CryptoMetricEvent> {
        val schemaRegistryClient: SchemaRegistryClient = CachedSchemaRegistryClient(kafkaConfig.schemaRegistryUrl, 3)
        return streamsBuilder(builder, kafkaConfig, schemaRegistryClient)
    }

    fun streamsBuilder(builder: StreamsBuilder, kafkaConfig: KafkaConfig, schemaRegistryClient: SchemaRegistryClient?): KStream<CryptoMetricEventWindow, CryptoMetricEvent>  {
        val stockSpecificAvroSerde = SpecificAvroSerde<Quote>(schemaRegistryClient)

        val cryptoMetricSpecificAvroSerde = SpecificAvroSerde<CryptoMetricEvent>(schemaRegistryClient)
        val cryptoMetricWindowSpecificAvroSerde = SpecificAvroSerde<CryptoMetricEventWindow>(schemaRegistryClient)

        val defaultSerdeConfig = Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            kafkaConfig.schemaRegistryUrl)

        stockSpecificAvroSerde.configure(defaultSerdeConfig, false)
        cryptoMetricSpecificAvroSerde.configure(defaultSerdeConfig,false)
        cryptoMetricWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)

        val stringSerde = Serdes.StringSerde()

        val topology: KStream<CryptoMetricEventWindow, CryptoMetricEvent> = builder.stream(kafkaConfig.quotesTopic, Consumed.with(
            stringSerde, stockSpecificAvroSerde,
            QuoteTimestampExtractor(), null))
            .groupByKey()
            .windowedBy(
                TimeWindows.of(Duration.ofMinutes(2))
                    .advanceBy(Duration.ofMinutes(1))
                    .grace(Duration.ZERO)
            )
            .aggregate(
                { cryptoMetric.identity() },
                { _, stc, aggregate -> cryptoMetric.aggregator(stc, aggregate)},
                Materialized.with(stringSerde, cryptoMetricSpecificAvroSerde)
            )
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
            .toStream()

            .selectKey{ key, _ -> cryptoMetric.windowBuilder(key.key(), key.window().end())}

        topology.to(kafkaConfig.cryptoMetricsTopic, Produced.with(cryptoMetricWindowSpecificAvroSerde, cryptoMetricSpecificAvroSerde))

        return topology
    }
}