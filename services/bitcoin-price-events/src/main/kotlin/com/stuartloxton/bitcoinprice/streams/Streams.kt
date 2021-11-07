package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.ATREvent
import com.stuartloxton.bitcoinprice.AveragePriceEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
import com.stuartloxton.bitcoinprice.config.KafkaConfig
import com.stuartloxton.bitcoinprice.streams.metrics.ATR
import com.stuartloxton.bitcoinprice.streams.metrics.AveragePrice
import com.stuartloxton.bitcoinpriceadapter.Stock
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*


@Component
class Streams {
    @Autowired
    private lateinit var averagePrice: AveragePrice

    @Autowired
    private lateinit var atr: ATR

    @Bean("kafkaStreamProcessing")
    fun startProcessing(@Qualifier("streamBuilder") builder: StreamsBuilder, kafkaConfig: KafkaConfig): KStream<BitcoinMetricEventWindow, BitcoinMetricEvent> {
        val schemaRegistryClient: SchemaRegistryClient = CachedSchemaRegistryClient(kafkaConfig.schemaRegistryUrl, 3)
        return streamsBuilder(builder, kafkaConfig, schemaRegistryClient)
    }

    fun joinMetricStreams(
        averagePriceStream: KStream<BitcoinMetricEventWindow, AveragePriceEvent>,
        atrStream: KStream<BitcoinMetricEventWindow, ATREvent>
    ): KStream<BitcoinMetricEventWindow, BitcoinMetricEvent> {
        val valueJoiner: ValueJoiner<AveragePriceEvent, ATREvent, BitcoinMetricEvent> =
            ValueJoiner { avgPrice: AveragePriceEvent,
                          atr: ATREvent ->
                BitcoinMetricEvent(avgPrice, atr)
            }

        val joinWindowSize = Duration.ofMinutes(5)
        val graceWindowSize = Duration.ofHours(10)
        val window = JoinWindows.of(joinWindowSize).grace(graceWindowSize)

        // Join Streams
        return averagePriceStream.join(atrStream, valueJoiner, window)

    }

    fun streamsBuilder(builder: StreamsBuilder, kafkaConfig: KafkaConfig, schemaRegistryClient: SchemaRegistryClient): KStream<BitcoinMetricEventWindow, BitcoinMetricEvent>  {
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

        val stream: KGroupedStream<String, Stock> = builder.stream(kafkaConfig.btcEventTopic, Consumed.with(
            stringSerde, stockSpecificAvroSerde,
            StockTimestampExtractor(), null))
            .groupByKey()

        // Financial Indicators
        val averagePrice: KStream<BitcoinMetricEventWindow, AveragePriceEvent> = averagePrice.topology(stream, schemaRegistryClient)
        val atr: KStream<BitcoinMetricEventWindow, ATREvent> = atr.topology(stream, schemaRegistryClient)

        // Merge Indicators
        val joinedStreams = joinMetricStreams(averagePrice, atr)

        joinedStreams.to(kafkaConfig.btcEventTopic, Produced.with(bitcoinMetricWindowSpecificAvroSerde, bitcoinMetricSpecificAvroSerde))
        return joinedStreams
    }
}