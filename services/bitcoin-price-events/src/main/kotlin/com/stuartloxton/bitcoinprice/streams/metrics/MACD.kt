package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinprice.MACDEvent
import com.stuartloxton.bitcoinpriceadapter.Stock
import org.springframework.stereotype.Component


@Component
class MACD: Metric<MACDEvent> {
    override fun empty(): MACDEvent = MACDEvent.newBuilder()
        .setAveragePrice(0.0)
        .setSumWindow(0.0)
        .setCountWindow(0)
        .setVolume(0.0)
        .build()

    override fun aggregator(newStock: Stock, current: Any): MACDEvent {
        val macdMetric = getMetric(current)
        val macdBuilder: MACDEvent.Builder = MACDEvent.newBuilder(macdMetric)
        // Calc Fields
        val sumWindow = macdMetric.getSumWindow() + newStock.getClose()
        val countWindow = macdMetric.getCountWindow() + 1
        val calcAvgPrice = sumWindow / countWindow

        // Set Fields
        val newMACD = macdBuilder
            .setSumWindow(sumWindow)
            .setCountWindow(countWindow)
            .setAveragePrice(calcAvgPrice)
            .setVolume(newStock.getVolume())

        // Build new AveragePrice object
        return newMACD.build()
    }

    override fun getMetric(event: Any): MACDEvent {
        return when (event) {
            is MACDEvent -> event
            else -> empty()
        }
    }
}