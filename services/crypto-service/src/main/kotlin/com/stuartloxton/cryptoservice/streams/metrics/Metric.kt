package com.stuartloxton.cryptoservice.streams.metrics

import com.stuartloxton.binance.gateway.Quote

interface Metric<T> {
    fun identity(): T
    fun aggregator(newQuote: Quote, current: Any): T
}