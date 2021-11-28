package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.binance.gateway.Quote
import com.stuartloxton.cryptoservice.AveragePriceEvent
import com.stuartloxton.cryptoservice.CryptoMetricEvent
import com.stuartloxton.cryptoservice.CryptoMetricEventWindow
import com.stuartloxton.cryptoservice.config.KafkaConfig
import com.stuartloxton.cryptoservice.streams.metrics.AveragePrice
import com.stuartloxton.cryptoservice.streams.metrics.CryptoMetric
import com.stuartloxton.cryptoservice.streams.metrics.ATR
import com.stuartloxton.cryptoservice.streams.Streams
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
import org.joda.time.DateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*


@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [
    KafkaConfig::class, Streams::class,
    CryptoMetric::class, AveragePrice::class,
    ATR::class
])
class StreamsTest {
    @Autowired
    private lateinit var kafkaConfigProperties: KafkaConfig

    @Autowired
    private lateinit var streams: Streams

    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, Quote>
    private lateinit var outputTopic: TestOutputTopic<CryptoMetricEventWindow, CryptoMetricEvent>

    val schemaRegistryClient: SchemaRegistryClient = MockSchemaRegistryClient()

    //Init Serdes
    private var quoteSpecificAvroSerde = SpecificAvroSerde<Quote>(schemaRegistryClient)
    private var cryptoMetricSpecificAvroSerde = SpecificAvroSerde<CryptoMetricEvent>(schemaRegistryClient)
    private var cryptoMetricWindowSpecificAvroSerde = SpecificAvroSerde<CryptoMetricEventWindow>(schemaRegistryClient)
    private var stringSerde = Serdes.StringSerde()


    /******************
     * UTIL FUNCTIONS
     *****************/

    fun getQuote(timestamp: Long, close: Double): Quote {
        return Quote(
            "BTC-AUD", timestamp,
            100.0,100.0,100.0,
            close, 100.0
            )
    }

    fun testTopologyBuilder(){
        val builder = StreamsBuilder()
        streams.streamsBuilder(builder, kafkaConfigProperties, schemaRegistryClient)
        val props = Properties()
        props.putAll(kafkaConfigProperties.getStreamsConfig())

        testDriver = TopologyTestDriver(builder.build(), props)

        inputTopic = testDriver.createInputTopic(
            kafkaConfigProperties.quotesTopic,
            stringSerde.serializer(),
            quoteSpecificAvroSerde.serializer()
        )
        outputTopic = testDriver.createOutputTopic(
            kafkaConfigProperties.cryptoMetricsTopic,
            cryptoMetricWindowSpecificAvroSerde.deserializer(),
            cryptoMetricSpecificAvroSerde.deserializer()
        )
    }

    fun configureSerdes(){
        val defaultSerdeConfig = Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaConfigProperties.schemaRegistryUrl)

        quoteSpecificAvroSerde.configure(defaultSerdeConfig, false)
        cryptoMetricSpecificAvroSerde.configure(defaultSerdeConfig,false)
        cryptoMetricWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)
    }

    fun pipeQuotes(ts: Long, n: Long, initClose:Double) {
        for (i in 0..n) {
            val close = initClose + (i*10)
            val time = ts + Duration.ofMinutes(i).toMillis()
            inputTopic.pipeInput("BTC-AUD", getQuote(time, close))
            inputTopic.advanceTime(Duration.ofMinutes(1))
        }
    }

    @BeforeEach
    fun setup() {
        configureSerdes()

        // Init topology testDriver
        testTopologyBuilder()
    }

    @AfterEach
    fun tearDown() {
        testDriver.close()
    }

    /*********
     * TESTS
     *********/

    @Test
    fun validateIfTestDriverCreated() {
        assertNotNull(testDriver);
    }

    @Test
    fun testStream() {
        val ts = SimpleDateFormat("yyyy-MM-dd")
            .parse("2020-01-01").time

        pipeQuotes(ts, 3,100.00)

        val rec = outputTopic.readRecord()
        val avg = rec.value.getAvgPrice() as AveragePriceEvent
        assertEquals(1, avg.getCountWindow())

        val windowEnd = DateTime(rec.key().getWindowEnd())
        val dateStr = windowEnd.toString("yyyy-MM-dd'T'HH:mm")
        assertEquals("2020-01-01T00:01", dateStr)
    }
}