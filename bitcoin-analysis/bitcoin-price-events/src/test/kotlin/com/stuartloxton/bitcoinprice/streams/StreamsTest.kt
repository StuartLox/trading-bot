//package com.stuartloxton.bitcoinprice.streams
//
//import com.stuartloxton.bitcoinprice.AveragePrice
//import com.stuartloxton.bitcoinprice.AveragePriceWindow
//import com.stuartloxton.bitcoinprice.Stock
//import com.stuartloxton.bitcoinprice.config.KafkaConfig
//import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
//import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
//import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
//import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
//import org.apache.kafka.clients.producer.ProducerRecord
//import org.apache.kafka.common.serialization.Serdes
//import org.apache.kafka.streams.StreamsBuilder
//import org.apache.kafka.streams.TopologyTestDriver
//import org.apache.kafka.streams.test.ConsumerRecordFactory
//import org.junit.After
//import org.junit.Assert.assertEquals
//import org.junit.Assert.assertNotNull
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.test.context.junit4.SpringRunner
//import java.util.*
//
//
//@RunWith(SpringRunner::class)
//@SpringBootTest(classes = [KafkaConfig::class],
//    properties = [
//        "application.kafka.btc-event-topic=test-btc-event-topic",
//        "application.kafka.avg-price-topic=test-avg-price-topic",
//        "application.kafka.bootstrap=dummy.bootstrap.com.localhost:9092",
//        "application.kafka.schema-registry=http://mock-registry:8081",
//        "application.kafka.group-id=streams-app.v1"
//    ])
//class StreamsTest {
//    @Autowired
//    private lateinit var kafkaConfigProperties: KafkaConfig
//
//    private lateinit var testDriver: TopologyTestDriver
//
//    val schemaRegistryClient: SchemaRegistryClient = MockSchemaRegistryClient()
//
//    //Init Serdes
//    private var stockSpecificAvroSerde = SpecificAvroSerde<Stock>(schemaRegistryClient)
//    private var avgPriceSpecificAvroSerde = SpecificAvroSerde<AveragePrice>(schemaRegistryClient)
//    private var avgPriceWindowSpecificAvroSerde = SpecificAvroSerde<AveragePriceWindow>(schemaRegistryClient)
//    private var stringSerde = Serdes.StringSerde()
//
//    //Data I/O
//    private var stockList = ArrayList<Stock>()
//    private var outputs = ArrayList<ProducerRecord<AveragePriceWindow, AveragePrice>>()
//
//    /******************
//     * UTIL FUNCTIONS
//     *****************/
//
//    //Pipes data into testDriver
//    fun pipeTestData() {
//        val factory = ConsumerRecordFactory<String, Stock>(
//            stringSerde.serializer(), stockSpecificAvroSerde.serializer())
//
//        for (i in 0 until stockList.size){
//            val record = factory.create(
//                kafkaConfigProperties.btc_event_topic,
//                stockList[i].getSymbol(), stockList[i])
//            testDriver.pipeInput(record)
//        }
//        testDriver.advanceWallClockTime(1000L)
//    }
//
//    //Generates test inputs.
//    fun setStockList(baseTimestamp: Long){
//        val baseClosePrice = 100.0
//        for (i in 0..5) {
//            val timestamp = baseTimestamp + (i * 20L)
//            val close = baseClosePrice + (i * 20.0)
//            val stock = Stock(
//                "BTC-AUD", timestamp,
//                100.0,100.0,100.0,
//                close, 100.0
//            )
//            stockList.add(stock)
//        }
//    }
//
//    fun setOutputs(){
//        for (i in 0 until stockList.size) {
//            val streamsEvent = testDriver.readOutput(
//                kafkaConfigProperties.avg_price_topic,
//                avgPriceWindowSpecificAvroSerde.deserializer(),
//                avgPriceSpecificAvroSerde.deserializer()
//            )
//           outputs.add(streamsEvent)
//        }
//    }
//
//    fun testTopologyBuilder(){
//        val builder = StreamsBuilder()
//        val streams = Streams()
//        streams.streamsBuilder(builder, kafkaConfigProperties, schemaRegistryClient)
//        val props = Properties()
//        props.putAll(kafkaConfigProperties.getStreamsConfig())
//
//        testDriver = TopologyTestDriver(builder.build(), props)
//    }
//
//    fun configureSerdes(){
//        val defaultSerdeConfig = Collections.singletonMap(
//            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaConfigProperties.schemaRegistryUrl)
//
//        stockSpecificAvroSerde.configure(defaultSerdeConfig, false)
//        avgPriceSpecificAvroSerde.configure(defaultSerdeConfig,false)
//        avgPriceWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)
//    }
//
//    /*********
//     * TESTS
//     *********/
//
//    @Before
//    fun setup() {
//        configureSerdes()
//
//        // Init topology testDriver
//        testTopologyBuilder()
//
//        // Define two sets of Stock objects
//        // from two distinct windows for testing.
//        val window1 = 1570752000L
//        val window2 = window1 + 86400L
//
//        setStockList(window1)
//        setStockList(window2)
//
//        // Pipe data into testDriver and
//        // extract outputs.
//        pipeTestData()
//        setOutputs()
//    }
//
//    @After
//    fun tearDown() {
//        testDriver.close()
//    }
//
//    @Test
//    fun validateIfTestDriverCreated() {
//        assertNotNull(testDriver);
//    }
//
//    @Test
//    fun assertStreamsNotNull() {
//        for (i in 0 until stockList.size) {
//            assertNotNull(outputs[i])
//        }
//    }
//
//    @Test
//    fun testFirstEvent(){
//        val avgPriceKey = outputs[0].key()
//        val avgPriceValue = outputs[0].value()
//
//        //Test Key
//        assertEquals("BTC-AUD", avgPriceKey.getSymbol())
//        assertEquals(1570838400000, avgPriceKey.getWindowEnd())
//
//        //Test Value
//        assertEquals(1, avgPriceValue.getCountWindow())
//        assertEquals(100.0, avgPriceValue.getSumWindow(), 0.0)
//        assertEquals(100.0, avgPriceValue.getAveragePrice(), 0.0)
//    }
//
//    @Test
//    fun testSecondEvent(){
//        val avgPriceKey = outputs[1].key()
//        val avgPriceValue = outputs[1].value()
//
//        //Test Key
//        assertEquals(1570838400000, avgPriceKey.getWindowEnd())
//
//        //Test Value
//        assertEquals(2, avgPriceValue.getCountWindow())
//        assertEquals(220.0, avgPriceValue.getSumWindow(),0.0)
//        assertEquals(110.0, avgPriceValue.getAveragePrice(),0.0)
//    }
//
//    @Test
//    fun testThirdEvent(){
//        val avgPriceKey = outputs[2].key()
//        val avgPriceValue = outputs[2].value()
//
//        //Test Key
//        assertEquals(1570838400000, avgPriceKey.getWindowEnd())
//
//        //Test Value
//        assertEquals(3, avgPriceValue.getCountWindow())
//        assertEquals(360.0, avgPriceValue.getSumWindow(),0.0)
//        assertEquals(120.0, avgPriceValue.getAveragePrice(),0.0)
//    }
//
//    @Test
//    fun testSecondWindow(){
//        val avgPriceKey = outputs[6].key()
//        val avgPriceValue = outputs[6].value()
//
//        //Test Key
//        assertEquals(1570924800000, avgPriceKey.getWindowEnd())
//
//        //Test Value
//        assertEquals(100.0, avgPriceValue.getSumWindow(), 0.0)
//        assertEquals(100.0, avgPriceValue.getAveragePrice(), 0.0)
//    }
//}