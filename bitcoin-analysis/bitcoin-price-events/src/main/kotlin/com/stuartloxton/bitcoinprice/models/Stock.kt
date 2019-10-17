//package com.stuartloxton.bitcoinprice.models
//
////data class Stock(
////    val symbol: String,
////    val timestamp: Long? = 0L,
////    val open: Double? = 0.0,
////    val high: Double? = 0.0,
////    val low: Double? = 0.0 ,
////    val close: Double? = 0.0,
////    val volume: Double? = 0.00
////)
//
//
//import javax.validation.constraints.NotBlank
//import javax.validation.constraints.NotNull
//
//
//class Stock  {
//
//    @NotNull(message = "Street number should not be null!")
//    @NotBlank(message = "Street number should not be empty!")
//    var symbol: String = ""
//
//    @NotNull(message = "Street name should not be null!")
//    @NotBlank(message = "Street name should not be empty!")
//    var volume: Double = 0.0
//
//    @NotNull(message = "Suburb should not be null!")
//    @NotBlank(message = "Suburb should not be empty!")
//    var close: Double = 0.0
//
//    @NotNull(message = "Post Code should not be null!")
//    @NotBlank(message = "Post Code should not be empty!")
//    var open: Double = 0.0
//
//    @NotNull(message = "State should not be null!")
//    @NotBlank(message = "State should not be empty!")
//    var high: Double = 0.0
//
//    @NotNull(message = "State should not be null!")
//    @NotBlank(message = "State should not be empty!")
//    var low: Double = 0.0
//
//    @NotNull(message = "Country should not be null!")
//    @NotBlank(message = "Country should not be empty!")
//    var timestamp: Long = 0L
//
//    constructor(symbol: String, timestamp: Long, open: Double, high: Double, low: Double, close: Double, volume: Double) {
//        this.symbol = symbol
//        this.timestamp = timestamp
//        this.open = open
//        this.high = high
//        this.close = close
//        this.low = low
//        this.volume = volume
//    }
//
//}