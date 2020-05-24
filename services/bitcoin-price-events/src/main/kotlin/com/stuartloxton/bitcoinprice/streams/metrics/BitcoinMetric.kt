package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinprice.BitcoinMetricEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
import org.springframework.beans.factory.annotation.Autowired

import com.stuartloxton.bitcoinpriceadapter.Stock
import org.springframework.stereotype.Component


@Component
class BitcoinMetric: Metric<BitcoinMetricEvent> {

    @Autowired
    lateinit var averagePrice: AveragePrice

    @Autowired
    private lateinit var atr: ATR

    override fun identity(): BitcoinMetricEvent = BitcoinMetricEvent.newBuilder()
        .setAtr(atr.identity())
        .setAvgPrice(averagePrice.identity())
        .build()

    override fun aggregator(newStock: Stock, current: Any): BitcoinMetricEvent {
        val currentMetricEvent = current as BitcoinMetricEvent
        val bitcoinMetricEventBuilder: BitcoinMetricEvent.Builder = BitcoinMetricEvent.newBuilder(currentMetricEvent)

        // Construct Metrics
        val avgPrice = averagePrice.aggregator(newStock, currentMetricEvent.getAvgPrice())
        val atrEvent = atr.aggregator(newStock,  currentMetricEvent.getAtr())

        // Set Fields
        val bitcoinMetrics = bitcoinMetricEventBuilder
            .setAvgPrice(avgPrice)
            .setAtr(atrEvent)
        // Build new Metrics object
        return bitcoinMetrics.build()
    }

    fun windowBuilder(symbol: String, windowEnd: Long): BitcoinMetricEventWindow {
        val avgPriceWindow = BitcoinMetricEventWindow.newBuilder()
            .setSymbol(symbol)
            .setWindowEnd(windowEnd)
            .build()
        return avgPriceWindow
    }
}