//package com.stuartloxton.bitcoinprice.routes
//
//import com.stuartloxton.bitcoinprice.Stock
//import com.stuartloxton.bitcoinprice.handler.StockHandler
//import com.stuartloxton.bitcoinprice.streams.StockEventProducer
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.mockito.Mockito.`when`
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
//import org.springframework.boot.test.mock.mockito.MockBean
//import org.springframework.http.MediaType
//import org.springframework.test.context.ContextConfiguration
//import org.springframework.test.context.junit4.SpringRunner
//import org.springframework.test.web.reactive.server.WebTestClient
//
//@RunWith(SpringRunner::class)
//@ContextConfiguration(classes=[StockRouter::class, StockHandler::class])
//@WebFluxTest
//class TestBitcoinStockRouter {
//
//    @Autowired
//    var webTestClient: WebTestClient? = null
//
//    @MockBean
//    private val stockEventProducer: StockEventProducer? = null
//
//    private lateinit var request: Stock
//
////    private lateinit var response: StockResponse
//
//    @Before
//    fun setup() {
//        request = Stock(
//            "BTC-AUD",123L,
//            0.0,0.0,0.0,
//            0.0,0.0
//        )
////        response = StockResponse("", any(), "Hello")
//    }
//
//    @Test
//    fun teststockEventProducer() {
//        val stock1 = getStock()
//        `when`(stockEventProducer!!.stockEventProducer(request)).thenReturn(true)
//        webTestClient!!.put()
//            .uri("/internal/quote/BTC-AUD")
//            .contentType(MediaType.APPLICATION_JSON)
//            .accept(MediaType.APPLICATION_JSON)
//            .exchange()
//            .expectStatus().is2xxSuccessful
//    }
//
//    fun getStock (): Stock {
//        val stock = Stock(
//            "BTC-AUD",123L,
//            0.0,0.0,0.0,
//            0.0,0.0
//        )
//        return stock
//    }
//}