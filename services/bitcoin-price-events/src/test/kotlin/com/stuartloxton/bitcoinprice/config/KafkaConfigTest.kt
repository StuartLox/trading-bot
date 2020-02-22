//package com.stuartloxton.bitcoinprice.config
//
//
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.context.properties.EnableConfigurationProperties
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.test.context.junit4.SpringRunner
//
//@RunWith(SpringRunner::class)
//@SpringBootTest(classes = [KafkaConfig::class])
//@EnableConfigurationProperties(value=[KafkaConfig::class])
//@ActiveProfiles("development")
//class KafkaConfigPropertiesTest {
//
//    @Autowired
//    lateinit var kafkaConfig: KafkaConfig
//
//    @Test
//    fun testProperties() {
//        assertThat(kafkaConfig.bootstrapUrl).isEqualTo("localhost:9092")
//        assertThat(kafkaConfig.btc_event_topic).isEqualTo("btc-event-topic")
//        assertThat(kafkaConfig.avg_price_topic).isEqualTo("avg-price-topic")
//        assertThat(kafkaConfig.groupId).isEqualTo("streams-app")
//        assertThat(kafkaConfig.schemaRegistryUrl).isEqualTo("http://localhost:8081")
//    }
//}
