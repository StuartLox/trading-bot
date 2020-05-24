package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.ATREvent
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

    fun initStocks(secs: Long, r: Int, close:Double) {
        var ts = 1577836800000L

        for (i in 1..r) {
            inputTopic.advanceTime(Duration.ofMillis(ts))
            inputTopic.pipeInput("BTC", getStock(ts, close))
            ts += Duration.ofSeconds(secs).toMillis()
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
    fun testAveragePrice() {
        initStocks(10, 100, 120.0)
        val rec = outputTopic.readRecord()
        val avg = rec.value.getAvgPrice() as AveragePriceEvent
        //Test Key
        assertEquals(1577836860000, rec.key().getWindowEnd())

        //Test Value
        assertEquals(720.0, avg.getSumWindow())
        assertEquals(6, avg.getCountWindow())
        assertEquals(120.0, avg.getAveragePrice())
    }

    @Test
    fun testAverageTrueRange() {
        initStocks(10, 100, 120.0)
        val rec = outputTopic.readRecord()
        val avg = rec.value.getAtr() as ATREvent
        //Test Key
        assertEquals(1577836860000, rec.key().getWindowEnd())

        //Test Value
        assertEquals(3.3333333333333335, avg.getAverageTrueRange())
    }


    @Test
    fun testStateStore() {
        initStocks(10, 100, 120.0)
//        val rec1 = outputTopic.readRecord()
//        val avg1 = rec1.value.getAvgPrice() as AveragePriceEvent
//        assertEquals(2, avg1.getCountWindow())
//        assertEquals("2019-10-13T09:43:00.000+11:00", DateTime(rec1.key().getWindowEnd()).toDateTimeISO().toString())
//
//        val rec2 = outputTopic.readRecord()
//        val avg2 = rec2.value.getAvgPrice() as AveragePriceEvent
//        assertEquals(2, avg2.getCountWindow())
//        assertEquals("2019-10-13T09:44:00.000+11:00", DateTime(rec2.key().getWindowEnd()).toDateTimeISO().toString())

//        val rec3 = outputTopic.readRecord()
//        assertEquals("2019-10-13T09:45:00.000+11:00", DateTime(rec3.key().getWindowEnd()).toDateTimeISO().toString())

        val rec = outputTopic.readRecordsToList()
        rec.forEach {
            println(DateTime(it.key().getWindowEnd()).toDateTimeISO().toString())
            println(it.value.getAtr() as ATREvent)
        }
//        assertEquals("2019-10-13T09:45:00.000+11:00", DateTime(rec3.key().getWindowEnd()).toDateTimeISO().toString())
    }
}