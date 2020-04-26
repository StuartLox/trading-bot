package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinprice.AveragePriceEvent
import com.stuartloxton.bitcoinpriceadapter.Stock
import org.springframework.stereotype.Component

@Component
class AveragePrice: Metric<AveragePriceEvent>  {
    override fun empty(): AveragePriceEvent = 
        AveragePriceEvent.newBuilder()
            .setAveragePrice(0.0)
            .setSumWindow(0.0)
            .setCountWindow(0)
            .setVolume(0.0)
            .build()
    
    override fun aggregator(newStock: Stock, current: Any): AveragePriceEvent {
        val avgPriceMetric = getMetric(current)
        val averagePriceBuilder: AveragePriceEvent.Builder = AveragePriceEvent.newBuilder(avgPriceMetric)
        // Calc Fields
        val sumWindow = avgPriceMetric.getSumWindow() + newStock.getClose()
        val countWindow = avgPriceMetric.getCountWindow() + 1
        val calcAvgPrice = sumWindow / countWindow

        // Set Fields
        val newAveragePrice = averagePriceBuilder
            .setSumWindow(sumWindow)
            .setCountWindow(countWindow)
            .setAveragePrice(calcAvgPrice)
            .setVolume(newStock.getVolume())

        // Build new AveragePrice object
        return newAveragePrice.build()
    }

    override fun getMetric(event: Any): AveragePriceEvent {
        return when (event) {
            is AveragePriceEvent -> event
            else -> empty()
        }
    }
}