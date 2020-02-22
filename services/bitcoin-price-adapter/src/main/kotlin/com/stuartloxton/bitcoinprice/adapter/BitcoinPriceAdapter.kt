package com.stuartloxton.bitcoinprice.adapter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class BitcoinPriceAdapter

fun main(args: Array<String>) {
	runApplication<BitcoinPriceAdapter>(*args)
}