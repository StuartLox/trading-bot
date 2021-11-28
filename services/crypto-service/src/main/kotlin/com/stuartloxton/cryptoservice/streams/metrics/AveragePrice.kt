package com.stuartloxton.cryptoservice.streams.metrics

import com.stuartloxton.binance.gateway.Quote
import com.stuartloxton.cryptoservice.AveragePriceEvent
import org.springframework.stereotype.Component

@Component
class AveragePrice: Metric<AveragePriceEvent> {

    override fun identity(): AveragePriceEvent =
        AveragePriceEvent.newBuilder()
            .setAveragePrice(0.0)
            .setSumWindow(0.0)
            .setCountWindow(0)
            .setVolume(0.0)
            .setClose(0.0)
            .build()

    override fun aggregator(newQuote: Quote, current: Any): AveragePriceEvent {
        val avgPriceMetric = current as AveragePriceEvent
        val averagePriceBuilder: AveragePriceEvent.Builder = AveragePriceEvent.newBuilder(avgPriceMetric)
        // Calc Fields
        val sumWindow = avgPriceMetric.getSumWindow() + newQuote.getClose()
        val countWindow = avgPriceMetric.getCountWindow() + 1
        val calcAvgPrice = sumWindow / countWindow

        // Set Fields
        val newAveragePrice = averagePriceBuilder
            .setSumWindow(sumWindow)
            .setCountWindow(countWindow)
            .setAveragePrice(calcAvgPrice)
            .setVolume(newQuote.getVolume())
            .setClose(newQuote.getClose())

        // Build new AveragePrice object
        return newAveragePrice.build()
    }
}