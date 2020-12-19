package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.AveragePriceEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
import com.stuartloxton.bitcoinprice.config.KafkaConfig
import com.stuartloxton.bitcoinprice.streams.metrics.AveragePrice
import com.stuartloxton.bitcoinprice.streams.metrics.BitcoinMetric
import com.stuartloxton.bitcoinprice.streams.metrics.ATR
import com.stuartloxton.bitcoinpriceadapter.Stock
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
    BitcoinMetric::class, AveragePrice::class,
    ATR::class
])
class StreamsTest {
    @Autowired
    private lateinit var kafkaConfigProperties: KafkaConfig

    @Autowired
    private lateinit var streams: Streams

    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String,Stock>
    private lateinit var outputTopic: TestOutputTopic<BitcoinMetricEventWindow,BitcoinMetricEvent>

    val schemaRegistryClient: SchemaRegistryClient = MockSchemaRegistryClient()

    //Init Serdes
    private var stockSpecificAvroSerde = SpecificAvroSerde<Stock>(schemaRegistryClient)
    private var btcEventMetricSpecificAvroSerde = SpecificAvroSerde<BitcoinMetricEvent>(schemaRegistryClient)
    private var btcEventMetricWindowSpecificAvroSerde = SpecificAvroSerde<BitcoinMetricEventWindow>(schemaRegistryClient)
    private var stringSerde = Serdes.StringSerde()


    /******************
     * UTIL FUNCTIONS
     *****************/

    fun getStock(timestamp: Long, close: Double): Stock {
        val stock = Stock(
            "BTC-AUD", timestamp,
            100.0,100.0,100.0,
            close, 100.0
            )
        return stock
    }

    fun testTopologyBuilder(){
        val builder = StreamsBuilder()
        streams.streamsBuilder(builder, kafkaConfigProperties, schemaRegistryClient)
        val props = Properties()
        props.putAll(kafkaConfigProperties.getStreamsConfig())

        testDriver = TopologyTestDriver(builder.build(), props)

        inputTopic = testDriver.createInputTopic(
            kafkaConfigProperties.btcEventTopic,
            stringSerde.serializer(),
            stockSpecificAvroSerde.serializer()
        )
        outputTopic = testDriver.createOutputTopic(
            kafkaConfigProperties.btcMetricsTopic,
            btcEventMetricWindowSpecificAvroSerde.deserializer(),
            btcEventMetricSpecificAvroSerde.deserializer()
        )
    }

    fun configureSerdes(){
        val defaultSerdeConfig = Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaConfigProperties.schemaRegistryUrl)

        stockSpecificAvroSerde.configure(defaultSerdeConfig, false)
        btcEventMetricSpecificAvroSerde.configure(defaultSerdeConfig,false)
        btcEventMetricWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)
    }

    fun pipeStocks(ts: Long, n: Long, initClose:Double) {
        for (i in 0..n) {
            val close = initClose + (i*10)
            val time = ts + Duration.ofMinutes(i).toMillis()
            inputTopic.pipeInput("BTC", getStock(time, close))
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

        pipeStocks(ts, 3,100.00)

        val rec = outputTopic.readRecord()
        val avg = rec.value.getAvgPrice() as AveragePriceEvent
        assertEquals(1, avg.getCountWindow())

        val windowEnd = DateTime(rec.key().getWindowEnd())
        val dateStr = windowEnd.toString("yyyy-MM-dd'T'HH:mm")
        assertEquals("2020-01-01T00:01", dateStr)
    }
}