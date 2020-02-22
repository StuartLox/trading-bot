package com.stuartloxton.bitcoinprice.routes

import com.stuartloxton.bitcoinprice.handler.StockHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router

@Configuration
class StockRouter(private val getStock: StockHandler) {

    @Bean
    fun route() = router {
        ("/internal").nest {
            PUT("/quote/{symbol}", getStock::handleStock)
        }
        ("/internal").nest {
            PUT("/quote/{symbol}/stream", getStock::startStream)
        }

    }
}
