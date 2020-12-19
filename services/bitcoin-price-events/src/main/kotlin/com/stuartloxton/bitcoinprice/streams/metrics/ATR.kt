package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinprice.ATREvent
import com.stuartloxton.bitcoinpriceadapter.Stock
import org.springframework.stereotype.Component
import java.lang.Math.abs


@Component
class ATR: Metric<ATREvent> {
    override fun identity(): ATREvent = ATREvent.newBuilder()
        .setAverageTrueRange(0.0)
        .setTrueRange(0.0)
        .setSumWindow(0.0)
        .setCountWindow(0)
        .setClose(0.0)
        .build()

    override fun aggregator(newStock: Stock, current: Any): ATREvent {
        val atrMetric = current as ATREvent
        val atrBuilder: ATREvent.Builder = ATREvent.newBuilder(atrMetric)
        // Calc Fields
        val countWindow = atrMetric.getCountWindow() + 1

        val v1 = newStock.getClose() - newStock.getLow()
        val v2 = abs(newStock.getHigh() - newStock.getClose())
        val v3 = abs(newStock.getLow() - newStock.getClose())

        val trueRange = maxOf(v1, v2, v3)
        val atr = trueRange / countWindow

        // Set Fields
        val newAtr = atrBuilder
            .setCountWindow(countWindow)
            .setClose(newStock.getClose())
            .setTrueRange(trueRange)
            .setAverageTrueRange(atr)

        // Build new AveragePrice object
        return newAtr.build()
    }
}