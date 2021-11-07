package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinprice.ATREvent
import com.stuartloxton.bitcoinprice.AveragePriceEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
import com.stuartloxton.bitcoinpriceadapter.Stock
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Math.abs
import java.time.Duration


@Component
class ATR: Metric<ATREvent> {
    override fun identity(): ATREvent = ATREvent.newBuilder()
        .setAverageTrueRange(0.0)
        .setTrueRange(0.0)
        .setSumWindow(0.0)
        .setCountWindow(0)
        .setClose(0.0)
        .build()

    override fun aggregator(newStock: Stock, current: ATREvent): ATREvent {
        val atrBuilder: ATREvent.Builder = ATREvent.newBuilder(current)
        // Calc Fields
        val countWindow = current.getCountWindow() + 1

        val v1 = newStock.getClose() - newStock.getLow()
        val v2 = abs(newStock.getHigh() - newStock.getClose())
        val v3 = abs(newStock.getLow() - newStock.getClose())

        val trueRange = maxOf(v1, v2, v3)
        val atr = trueRange / countWindow

        // Set Fields
        return atrBuilder
            .setCountWindow(countWindow)
            .setClose(newStock.getClose())
            .setTrueRange(trueRange)
            .setAverageTrueRange(atr)
            .build()
    }

    override fun topology(stream: KGroupedStream<String, Stock>, schemaRegistryClient: SchemaRegistryClient): KStream<BitcoinMetricEventWindow, ATREvent> {
        val stringSerde = Serdes.StringSerde()
        val averPriceAvroSerde = SpecificAvroSerde<ATREvent>(schemaRegistryClient)

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