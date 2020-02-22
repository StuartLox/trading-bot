//package com.stuartloxton.bitcoinprice.streams
//
//import com.stuartloxton.bitcoinprice.Stock
//import com.stuartloxton.bitcoinprice.config.KafkaConfig
//import org.junit.ClassRule
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.mock.mockito.MockBean
//import org.springframework.kafka.test.rule.EmbeddedKafkaRule
//import org.springframework.test.context.junit4.SpringRunner
//
//@SpringBootTest
//@RunWith(SpringRunner::class)
//class StockEventProducerTest {
//    companion object {
//        @ClassRule
//        @JvmField
//        val embeddedKafka = EmbeddedKafkaRule(1, false, "btc-event-topic")
//    }
//
//    @MockBean
//    private lateinit var kafkaConfigProperties: KafkaConfig
//
//    @MockBean
//    private lateinit var stockEventProducer: StockEventProducer
//
//    @Test
//    fun testSend(){
//        val stock = Stock(
//            "BTC-AUD",123L,
//            0.0,0.0,0.0,
//            0.0,0.0
//        )
//        stockEventProducer.stockEventProducer(stock)
//    }
//}