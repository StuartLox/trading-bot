package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.AveragePrice
import com.stuartloxton.bitcoinprice.AveragePriceWindow
import com.stuartloxton.bitcoinpriceadapter.Stock
import com.stuartloxton.bitcoinprice.config.KafkaConfig
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
class Streams {

    @Bean("kafkaStreamProcessing")
    fun startProccessing(@Qualifier("app1StreamBuilder") builder: StreamsBuilder, kafkaConfig: KafkaConfig): KStream<AveragePriceWindow, AveragePrice> {
        val schemaRegistryClient: SchemaRegistryClient = CachedSchemaRegistryClient(kafkaConfig.schemaRegistryUrl, 3)
        return streamsBuilder(builder, kafkaConfig, schemaRegistryClient)
    }

    fun streamsBuilder(builder: StreamsBuilder, kafkaConfig: KafkaConfig, schemaRegistryClient: SchemaRegistryClient?): KStream<AveragePriceWindow, AveragePrice>  {
        val stockSpecificAvroSerde = SpecificAvroSerde<Stock>(schemaRegistryClient)
        val avgPriceSpecificAvroSerde = SpecificAvroSerde<AveragePrice>(schemaRegistryClient)
        val avgPriceWindowSpecificAvroSerde = SpecificAvroSerde<AveragePriceWindow>(schemaRegistryClient)

        val defaultSerdeConfig = Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            kafkaConfig.schemaRegistryUrl)

        stockSpecificAvroSerde.configure(defaultSerdeConfig, false)
        avgPriceSpecificAvroSerde.configure(defaultSerdeConfig,false)
        avgPriceWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)

        val stringSerde = Serdes.StringSerde()

        fun emptyAveragePrice(): AveragePrice = AveragePrice.newBuilder()
            .setAveragePrice(0.0)
            .setSumWindow(0.0)
            .setCountWindow(0)
            .build()

        fun averagePriceAggregator(newStock: Stock, currentAveragePrice: AveragePrice): AveragePrice {
            val averagePriceBuilder: AveragePrice.Builder = AveragePrice.newBuilder(currentAveragePrice)
            // Calc Fields
            val sumWindow = currentAveragePrice.getSumWindow() + newStock.getClose()
            val countWindow = currentAveragePrice.getCountWindow() + 1
            val calcAvgPrice = sumWindow / countWindow

            // Set Fields
            val newAveragePrice = averagePriceBuilder
                .setSumWindow(sumWindow)
                .setCountWindow(countWindow)
                .setAveragePrice(calcAvgPrice)

            // Build new AveragePrice object
            return newAveragePrice.build()
        }

        fun averagePriceWindowBuilder(symbol: String, windowEnd: Long): AveragePriceWindow {
            val avgPriceWindow = AveragePriceWindow.newBuilder()
                .setSymbol(symbol)
                .setWindowEnd(windowEnd)
                .build()
            return avgPriceWindow
        }

        val movingAvgPrice: KStream<AveragePriceWindow, AveragePrice> = builder.stream(kafkaConfig.btc_event_topic, Consumed.with(
            stringSerde, stockSpecificAvroSerde,
            StockTimestampExtractor(), null))
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMillis(Dimension4.minute * 5)))
            .aggregate(
                { emptyAveragePrice() },
                { _, stc, aggregate -> averagePriceAggregator(stc, aggregate)},
                Materialized.with(stringSerde,avgPriceSpecificAvroSerde)
            )
//            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
            .toStream()
            .selectKey{ key, _ -> averagePriceWindowBuilder(key.key(), key.window().end())}
            .peek{ key, value ->
                println(key)
                println(value)
            }

        movingAvgPrice.to(kafkaConfig.avg_price_topic, Produced.with(avgPriceWindowSpecificAvroSerde, avgPriceSpecificAvroSerde))
        return movingAvgPrice
    }
}