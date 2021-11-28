package com.stuartloxton.cryptoservice.streams.metrics

import com.stuartloxton.binance.gateway.Quote
import org.springframework.beans.factory.annotation.Autowired

import com.stuartloxton.cryptoservice.CryptoMetricEvent
import com.stuartloxton.cryptoservice.CryptoMetricEventWindow
import org.springframework.stereotype.Component


@Component
class CryptoMetric: Metric<CryptoMetricEvent> {

    @Autowired
    lateinit var averagePrice: AveragePrice

    @Autowired
    private lateinit var atr: ATR

    override fun identity(): CryptoMetricEvent = CryptoMetricEvent.newBuilder()
        .setAtr(atr.identity())
        .setAvgPrice(averagePrice.identity())
        .build()

    override fun aggregator(newQuote: Quote, current: Any): CryptoMetricEvent {
        val currentMetricEvent = current as CryptoMetricEvent
        val cryptoMetricEventBuilder: CryptoMetricEvent.Builder = CryptoMetricEvent.newBuilder(currentMetricEvent)

        // Construct Metrics
        val avgPrice = averagePrice.aggregator(newQuote, currentMetricEvent.getAvgPrice())
        val atrEvent = atr.aggregator(newQuote,  currentMetricEvent.getAtr())

        // Set Fields
        val bitcoinMetrics = cryptoMetricEventBuilder
            .setAvgPrice(avgPrice)
            .setAtr(atrEvent)
        // Build new Metrics object
        return bitcoinMetrics.build()
    }

    fun windowBuilder(symbol: String, windowEnd: Long): CryptoMetricEventWindow {
        return CryptoMetricEventWindow.newBuilder()
            .setSymbol(symbol)
            .setWindowEnd(windowEnd)
            .build()
    }
}