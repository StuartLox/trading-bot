//package com.stuartloxton.bitcoinprice.streams.metrics
//
//import com.stuartloxton.bitcoinprice.BitcoinMetricEvent
//import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
//import com.stuartloxton.bitcoinprice.config.KafkaConfig
//import org.springframework.beans.factory.annotation.Autowired
//
//import com.stuartloxton.bitcoinpriceadapter.Stock
//import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
//import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
//import org.apache.kafka.common.serialization.Serdes
//import org.apache.kafka.streams.kstream.*
//import org.springframework.stereotype.Component
//import java.time.Duration
//
//
//@Component
//class BitcoinMetric: Metric<BitcoinMetricEvent> {
//
//    @Autowired
//    lateinit var averagePrice: AveragePrice
//
//    @Autowired
//    private lateinit var atr: ATR
//
//    override fun identity(): BitcoinMetricEvent = BitcoinMetricEvent.newBuilder()
//        .setAtr(atr.identity())
//        .setAvgPrice(averagePrice.identity())
//        .build()
//
//    override fun aggregator(newStock: Stock, current: BitcoinMetricEvent): BitcoinMetricEvent {
//        val bitcoinMetricEventBuilder: BitcoinMetricEvent.Builder = BitcoinMetricEvent.newBuilder(current)
//
//        // Construct Metrics
//        val avgPrice = averagePrice.aggregator(newStock, current.getAvgPrice())
//        val atrEvent = atr.aggregator(newStock,  current.getAtr())
//
//        return bitcoinMetricEventBuilder
//            .setAvgPrice(avgPrice)
//            .setAtr(atrEvent)
//            .build()
//    }
//
//    override fun topology(stream: KGroupedStream<String, Stock>): KStream<BitcoinMetricEventWindow, BitcoinMetricEvent> {
//        val stringSerde = Serdes.StringSerde()
//        val bitcoinMetricSpecificAvroSerde = SpecificAvroSerde<BitcoinMetricEvent>(schemaRegistryClient)
//
//        return stream.windowedBy(
//                TimeWindows.of(Duration.ofMinutes(2))
//                    .advanceBy(Duration.ofMinutes(1))
//                    .grace(Duration.ZERO)
//            )
//            .aggregate(
//                { identity() },
//                { _, stc, aggregate -> aggregator(stc, aggregate)},
//                Materialized.with(stringSerde, bitcoinMetricSpecificAvroSerde)
//            )
//            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
//            .toStream()
//            .selectKey{ key, _ -> windowBuilder(key.key(), key.window().end())}
//    }
//
//    fun windowBuilder(symbol: String, windowEnd: Long): BitcoinMetricEventWindow {
//        return BitcoinMetricEventWindow.newBuilder()
//            .setSymbol(symbol)
//            .setWindowEnd(windowEnd)
//            .build()
//    }
//}