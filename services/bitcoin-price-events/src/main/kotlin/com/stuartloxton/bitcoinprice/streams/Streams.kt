package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.*
import com.stuartloxton.bitcoinpriceadapter.Stock
import com.stuartloxton.bitcoinprice.config.KafkaConfig
import com.stuartloxton.bitcoinprice.streams.metrics.AveragePrice
import com.stuartloxton.bitcoinprice.streams.metrics.MACD
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
class Streams {

    @Autowired
    private lateinit var averagePrice: AveragePrice

    @Autowired
    private lateinit var macd: MACD

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

        fun emptyMetric(): BitcoinMetricEvent = BitcoinMetricEvent.newBuilder()
            .setMacd(averagePrice.empty())
            .setAvgPrice(macd.empty())
            .build()

        fun bitcoinMetricAggregator(newStock: Stock, currentMetric: BitcoinMetricEvent): BitcoinMetricEvent {
            val bitcoinMetricEventBuilder: BitcoinMetricEvent.Builder = BitcoinMetricEvent.newBuilder(currentMetric)
            // Construct Metrics
            val avgPrice = averagePrice.aggregator(newStock, currentMetric.getAvgPrice())
            val macdEvent = macd.aggregator(newStock, currentMetric.getMacd())

            // Set Fields
            val bitcoinMetrics = bitcoinMetricEventBuilder
                .setAvgPrice(avgPrice)
                .setMacd(macdEvent)
            // Build new Metrics object
            return bitcoinMetrics.build()
        }

        fun bitcoinMetricWindowBuilder(symbol: String, windowEnd: Long): BitcoinMetricEventWindow {
            val avgPriceWindow = BitcoinMetricEventWindow.newBuilder()
                .setSymbol(symbol)
                .setWindowEnd(windowEnd)
                .build()
            return avgPriceWindow
        }
        // Moving Average
        val btcMetricsStream: KStream<BitcoinMetricEventWindow, BitcoinMetricEvent> = builder.stream(kafkaConfig.btcEventTopic, Consumed.with(
            stringSerde, stockSpecificAvroSerde,
            StockTimestampExtractor(), null))
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMillis(Dimension4.minute)).grace(Duration.ZERO))
            .aggregate(
                { emptyMetric() },
                { _, stc, aggregate -> bitcoinMetricAggregator(stc, aggregate)},
                Materialized.with(stringSerde, bitcoinMetricSpecificAvroSerde)
            )
//            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
            .toStream()
            .selectKey{ key, _ -> bitcoinMetricWindowBuilder(key.key(), key.window().end())}

        btcMetricsStream.to(kafkaConfig.btcMetricTopic, Produced.with(bitcoinMetricWindowSpecificAvroSerde, bitcoinMetricSpecificAvroSerde))

        return btcMetricsStream

    }
}