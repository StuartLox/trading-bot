package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.BitcoinMetricEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
import com.stuartloxton.bitcoinprice.config.KafkaConfig
import com.stuartloxton.bitcoinprice.streams.metrics.BitcoinMetric
import com.stuartloxton.bitcoinpriceadapter.Stock
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
    private lateinit var bitcoinMetric: BitcoinMetric

    @Bean("kafkaStreamProcessing")
    fun startProccessing(@Qualifier("streamBuilder") builder: StreamsBuilder, kafkaConfig: KafkaConfig): KStream<BitcoinMetricEventWindow, BitcoinMetricEvent> {
        val schemaRegistryClient: SchemaRegistryClient = CachedSchemaRegistryClient(kafkaConfig.schemaRegistryUrl, 3)
        return streamsBuilder(builder, kafkaConfig, schemaRegistryClient)
    }

    fun streamsBuilder(builder: StreamsBuilder, kafkaConfig: KafkaConfig, schemaRegistryClient: SchemaRegistryClient?): KStream<BitcoinMetricEventWindow, BitcoinMetricEvent>  {
        val stockSpecificAvroSerde = SpecificAvroSerde<Stock>(schemaRegistryClient)

        val bitcoinMetricSpecificAvroSerde = SpecificAvroSerde<BitcoinMetricEvent>(schemaRegistryClient)
        val bitcoinMetricWindowSpecificAvroSerde = SpecificAvroSerde<BitcoinMetricEventWindow>(schemaRegistryClient)

        val defaultSerdeConfig = Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            kafkaConfig.schemaRegistryUrl)

        stockSpecificAvroSerde.configure(defaultSerdeConfig, false)
        bitcoinMetricSpecificAvroSerde.configure(defaultSerdeConfig,false)
        bitcoinMetricWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)

        val stringSerde = Serdes.StringSerde()

        val topology: KStream<BitcoinMetricEventWindow, BitcoinMetricEvent> = builder.stream(kafkaConfig.btcEventTopic, Consumed.with(
            stringSerde, stockSpecificAvroSerde,
            StockTimestampExtractor(), null))
            .groupByKey()
            .windowedBy(
                TimeWindows.of(Duration.ofMinutes(2))
                    .advanceBy(Duration.ofMinutes(1))
                    .grace(Duration.ZERO)
            )

            .aggregate(
                { bitcoinMetric.identity() },
                { _, stc, aggregate -> bitcoinMetric.aggregator(stc, aggregate)},
                Materialized.with(stringSerde, bitcoinMetricSpecificAvroSerde)
            )
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
            .toStream()

            .selectKey{ key, _ -> bitcoinMetric.windowBuilder(key.key(), key.window().end())}

        topology.to(kafkaConfig.btcMetricsTopic, Produced.with(bitcoinMetricWindowSpecificAvroSerde, bitcoinMetricSpecificAvroSerde))

        return topology

    }
}