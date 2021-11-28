package com.stuartloxton.cryptoprice.adapter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class CryptoPriceProcessor

fun main(args: Array<String>) {
	runApplication<CryptoPriceProcessor>(*args)
}