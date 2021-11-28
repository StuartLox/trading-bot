package com.stuartloxton.bitcoinprice.config


import com.stuartloxton.cryptoservice.config.KafkaConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [KafkaConfig::class])
class KafkaConfigPropertiesTest {

    @Autowired
    lateinit var kafkaConfig: KafkaConfig

    @Test
    fun testProperties() {
        assertThat(kafkaConfig.bootstrapUrl).isEqualTo("localhost:9092")
        assertThat(kafkaConfig.quotesTopic).isEqualTo("test-btc-events")
        assertThat(kafkaConfig.cryptoMetricsTopic).isEqualTo("test-btc-metrics")
        assertThat(kafkaConfig.groupId).isEqualTo("test-streams.v1")
        assertThat(kafkaConfig.schemaRegistryUrl).isEqualTo("http://localhost:8081")
    }
}
