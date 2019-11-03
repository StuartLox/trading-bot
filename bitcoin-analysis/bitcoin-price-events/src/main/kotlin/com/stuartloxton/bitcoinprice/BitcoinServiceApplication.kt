package com.stuartloxton.bitcoinprice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication()
class BitcoinServiceApplication

fun main(args: Array<String>) {
    runApplication<BitcoinServiceApplication>(*args)
}