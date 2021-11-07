package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinprice.AveragePriceEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
import com.stuartloxton.bitcoinpriceadapter.Stock
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class AveragePrice: Metric<AveragePriceEvent>  {
    override fun identity(): AveragePriceEvent =
        AveragePriceEvent.newBuilder()
            .setAveragePrice(0.0)
            .setSumWindow(0.0)
            .setCountWindow(0)
            .setVolume(0.0)
            .setClose(0.0)
            .build()

    override fun aggregator(newStock: Stock, current: AveragePriceEvent): AveragePriceEvent {
        val averagePriceBuilder: AveragePriceEvent.Builder = AveragePriceEvent.newBuilder(current)
        // Calc Fields
        val sumWindow = current.getSumWindow() + newStock.getClose()
        val countWindow = current.getCountWindow() + 1
        val calcAvgPrice = sumWindow / countWindow

        // Set Fields
        val newAveragePrice = averagePriceBuilder
            .setSumWindow(sumWindow)
            .setCountWindow(countWindow)
            .setAveragePrice(calcAvgPrice)
            .setVolume(newStock.getVolume())
            .setClose(newStock.getClose())

        // Build new AveragePrice object
        return newAveragePrice.build()
    }

    override fun topology(stream: KGroupedStream<String, Stock>, schemaRegistryClient: SchemaRegistryClient): KStream<BitcoinMetricEventWindow, AveragePriceEvent> {
        val stringSerde = Serdes.StringSerde()
        val averPriceAvroSerde = SpecificAvroSerde<AveragePriceEvent>(schemaRegistryClient)

        return stream.windowedBy(
            TimeWindows.of(Duration.ofMinutes(2))
                .advanceBy(Duration.ofMinutes(1))
                .grace(Duration.ZERO)
        )
            .aggregate(
                { identity() },
                { _, stc, aggregate -> aggregator(stc, aggregate)},
                Materialized.with(stringSerde, averPriceAvroSerde)
            )
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
            .toStream()
            .selectKey{ key, _ -> windowBuilder(key.key(), key.window().end())}

    }

    fun windowBuilder(symbol: String, windowEnd: Long): BitcoinMetricEventWindow {
        return BitcoinMetricEventWindow.newBuilder()
            .setSymbol(symbol)
            .setWindowEnd(windowEnd)
            .build()
    }
}