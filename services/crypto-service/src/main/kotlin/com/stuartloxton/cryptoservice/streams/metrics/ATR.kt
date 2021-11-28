package com.stuartloxton.cryptoservice.streams.metrics

import com.stuartloxton.binance.gateway.Quote
import com.stuartloxton.cryptoservice.ATREvent
import org.springframework.stereotype.Component
import kotlin.math.abs


@Component
class ATR: Metric<ATREvent> {
    override fun identity(): ATREvent = ATREvent.newBuilder()
        .setAverageTrueRange(0.0)
        .setTrueRange(0.0)
        .setSumWindow(0.0)
        .setCountWindow(0)
        .setClose(0.0)
        .build()

    override fun aggregator(newQuote: Quote, current: Any): ATREvent {
        val atrMetric = current as ATREvent
        val atrBuilder: ATREvent.Builder = ATREvent.newBuilder(atrMetric)
        // Calc Fields
        val countWindow = atrMetric.getCountWindow() + 1

        val v1 = newQuote.getClose() - newQuote.getLow()
        val v2 = abs(newQuote.getHigh() - newQuote.getClose())
        val v3 = abs(newQuote.getLow() - newQuote.getClose())

        val trueRange = maxOf(v1, v2, v3)
        val atr = trueRange / countWindow

        // Set Fields
        val newAtr = atrBuilder
            .setCountWindow(countWindow)
            .setClose(newQuote.getClose())
            .setTrueRange(trueRange)
            .setAverageTrueRange(atr)

        // Build new AveragePrice object
        return newAtr.build()
    }
}