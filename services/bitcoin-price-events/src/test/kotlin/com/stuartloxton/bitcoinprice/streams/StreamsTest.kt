package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.AveragePriceEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEvent
import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
import com.stuartloxton.bitcoinprice.config.KafkaConfig
import com.stuartloxton.bitcoinprice.streams.metrics.AveragePrice
import com.stuartloxton.bitcoinprice.streams.metrics.BitcoinMetric
import com.stuartloxton.bitcoinprice.streams.metrics.MACD
import com.stuartloxton.bitcoinpriceadapter.Stock
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*


@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [
    KafkaConfig::class, Streams::class,
    BitcoinMetric::class, AveragePrice::class,
    MACD::class
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

    /*********
     * TESTS
     *********/

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

    @Test
    fun validateIfTestDriverCreated() {
        assertNotNull(testDriver);
    }

    @Test
    fun testOneMinSuppression() {
        val ts = 1570800000L
        inputTopic.pipeInput("BTC", getStock(ts, 100.0))
        assertEquals(outputTopic.isEmpty, true)
        val t = 60 * 1000L
        inputTopic.pipeInput("BTC", getStock(ts + t, 100.0))
        assertEquals(outputTopic.isEmpty, false)
    }

    @Test
    fun testAveragePriceWindow() {
        val ts = 1570800000L
        inputTopic.pipeInput("BTC", getStock(ts, 100.0))
        inputTopic.pipeInput("BTC", getStock(ts+20 * 1000L, 120.0))
        inputTopic.pipeInput("BTC", getStock(ts + 60 * 1000L, 120.0))

        val rec = outputTopic.readRecord()
        val avg = rec.value.getAvgPrice() as AveragePriceEvent
        //Test Key
        assertEquals(1570860000, rec.key().getWindowEnd())

        //Test Value
        assertEquals(220.0, avg.getSumWindow())
        assertEquals(2, avg.getCountWindow())
        assertEquals(110.0, avg.getAveragePrice())
    }
}