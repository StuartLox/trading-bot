package com.stuartloxton.bitcoinprice.serdes

data class Stocks(
    val symbol: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)
