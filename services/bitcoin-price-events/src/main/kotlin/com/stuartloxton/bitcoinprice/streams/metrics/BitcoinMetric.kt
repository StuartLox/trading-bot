package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinprice.BitcoinMetricEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
import org.springframework.beans.factory.annotation.Autowired

import com.stuartloxton.bitcoinpriceadapter.Stock
import org.springframework.stereotype.Component


@Component
class BitcoinMetric: Metric<BitcoinMetricEvent> {

    @Autowired
    private lateinit var averagePrice: AveragePrice

    @Autowired
    private lateinit var macd: MACD

    override fun empty(): BitcoinMetricEvent = BitcoinMetricEvent.newBuilder()
        .setMacd(macd.empty())
        .setAvgPrice(averagePrice.empty())
        .build()

    override fun aggregator(newStock: Stock, current: Any): BitcoinMetricEvent {
        val currentMetricEvent = getMetric(current)
        val bitcoinMetricEventBuilder: BitcoinMetricEvent.Builder = BitcoinMetricEvent.newBuilder(currentMetricEvent)

        // Construct Metrics
        val avgPrice = averagePrice.aggregator(newStock, currentMetricEvent.getAvgPrice())
        val macdEvent = macd.aggregator(newStock,  currentMetricEvent.getMacd())

        // Set Fields
        val bitcoinMetrics = bitcoinMetricEventBuilder
            .setAvgPrice(avgPrice)
            .setMacd(macdEvent)
        // Build new Metrics object
        return bitcoinMetrics.build()
    }

    override fun getMetric(event: Any): BitcoinMetricEvent {
        return when (event) {
            is BitcoinMetricEvent -> event
            else -> empty()
        }
    }

    fun windowBuilder(symbol: String, windowEnd: Long): BitcoinMetricEventWindow {
        val avgPriceWindow = BitcoinMetricEventWindow.newBuilder()
            .setSymbol(symbol)
            .setWindowEnd(windowEnd)
            .build()
        return avgPriceWindow
    }
}