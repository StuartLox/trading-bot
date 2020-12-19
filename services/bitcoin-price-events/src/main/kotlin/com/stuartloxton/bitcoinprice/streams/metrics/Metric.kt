package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinpriceadapter.Stock

interface Metric<T> {
    fun identity(): T
    fun aggregator(newStock: Stock, current: Any): T
}