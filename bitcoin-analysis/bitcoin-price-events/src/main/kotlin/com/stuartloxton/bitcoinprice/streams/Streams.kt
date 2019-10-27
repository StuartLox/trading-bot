package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.AveragePrice
import com.stuartloxton.bitcoinprice.AveragePriceWindow
import com.stuartloxton.bitcoinprice.Stock
import com.stuartloxton.bitcoinprice.config.KafkaConfig
import com.stuartloxton.bitcoinprice.serdes.StockTimestampExtractor
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.TimeWindows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*


@Component
class Streams {

    @Autowired
    @Qualifier("app1StreamBuilder")
    private lateinit var builder: StreamsBuilder

    private lateinit var kafkaConfig: KafkaConfig

    private val stockSpecificAvroSerde = SpecificAvroSerde<Stock>()
    private val avgPriceSpecificAvroSerde = SpecificAvroSerde<AveragePrice>()
    private val avgPriceWindowSpecificAvroSerde = SpecificAvroSerde<AveragePriceWindow>()

    fun startProcessing(): KStream<AveragePriceWindow, AveragePrice> {

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
            val sumWindow = currentAveragePrice.getAveragePrice() + newStock.getClose()
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
            .windowedBy(TimeWindows.of(Duration.ofMillis(Dimension4.day).toMillis()))
            .aggregate(
                { emptyAveragePrice() },
                { _, stc, aggregate -> averagePriceAggregator(stc, aggregate) }
            )
            .toStream()
            .selectKey{ key, _ -> averagePriceWindowBuilder(key.key(), key.window().end())}

        movingAvgPrice.to(kafkaConfig.avg_price_topic, Produced.with(avgPriceWindowSpecificAvroSerde, avgPriceSpecificAvroSerde))
        return movingAvgPrice
    }
}