package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinpriceadapter.Stock

interface Metric<T> {
    fun empty(): T
    fun aggregator(newStock: Stock, current: Any): T
    fun getMetric(event: Any): T
}