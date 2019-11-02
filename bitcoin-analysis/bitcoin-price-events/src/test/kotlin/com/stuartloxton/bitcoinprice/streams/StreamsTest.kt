package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.AveragePrice
import com.stuartloxton.bitcoinprice.AveragePriceWindow
import com.stuartloxton.bitcoinprice.Stock
import com.stuartloxton.bitcoinprice.config.KafkaConfig
import com.stuartloxton.bitcoinprice.serdes.StockTimestampExtractor
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.time.Duration
import java.util.*



@EnableConfigurationProperties(value=[KafkaConfig::class])
@DirtiesContext
@ActiveProfiles("development")
@SpringBootTest
@RunWith(SpringRunner::class)
class StreamsTest {
    @Autowired
    private lateinit var kafkaConfigProperties: KafkaConfig

    @MockBean
    private lateinit var stockEventProducer: StockEventProducer

    @MockBean
    private lateinit var streams: Streams

    @MockBean
    private lateinit var testDriver: TopologyTestDriver


    val schemaRegistryClient: SchemaRegistryClient = MockSchemaRegistryClient()

    val stringSerde = Serdes.StringSerde()

    val INPUT_TOPIC = "TestTopicInput"
    val OUTPUT_TOPIC = "TestTopicOutput"
    private var schemaRegistryUrl = "http://dummy:8081"

    private var stockSpecificAvroSerde = SpecificAvroSerde<Stock>(schemaRegistryClient)
    private var avgPriceSpecificAvroSerde = SpecificAvroSerde<AveragePrice>(schemaRegistryClient)
    private var avgPriceWindowSpecificAvroSerde = SpecificAvroSerde<AveragePriceWindow>(schemaRegistryClient)

    @Before
    fun setup() {
        val defaultSerdeConfig = Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)

        stockSpecificAvroSerde.configure(defaultSerdeConfig, false)
        avgPriceSpecificAvroSerde.configure(defaultSerdeConfig,false)
        avgPriceWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)

        val builder = startProcessing()


        val props = Properties()
        props.putAll(kafkaConfigProperties.getStreamsConfig())

        testDriver = TopologyTestDriver(builder.build(), props)
    }

    fun readOutput(): ProducerRecord<AveragePriceWindow, AveragePrice> {
        val output = testDriver.readOutput(kafkaConfigProperties.avg_price_topic,
            avgPriceWindowSpecificAvroSerde.deserializer(), avgPriceSpecificAvroSerde.deserializer())
        return output
    }

    @After
    fun tearDown() {
        testDriver.close()
    }

    @Test
    fun validateIfTestDriverCreated() {
        assertNotNull(testDriver);
    }

    @Test
    fun testSend(){
        val stock1 = Stock(
            "BTC-AUD",1572177505L,
            100.0,100.0,100.0,
            200.0,100.0
        )

        val stock2 = Stock(
            "BTC-AUD",157222079L,
            100.0,100.0,100.0,
            110.0,100.0
        )

        val stockSerializer = stockSpecificAvroSerde.serializer()
        stockSerializer.configure(Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl), false)

        val factory = ConsumerRecordFactory<String, Stock>(stringSerde.serializer(), stockSerializer)
        val record1 = factory.create(kafkaConfigProperties.btc_event_topic, stock1.getSymbol(), stock1 )
        val record2 = factory.create(kafkaConfigProperties.btc_event_topic, stock2.getSymbol(), stock2)

        testDriver.pipeInput(factory.create(kafkaConfigProperties.btc_event_topic, stock1.getSymbol(), stock1, 1L ))
        testDriver.pipeInput(factory.create(kafkaConfigProperties.btc_event_topic, stock2.getSymbol(), stock2, 2L))
        testDriver.pipeInput(factory.create(kafkaConfigProperties.btc_event_topic, stock2.getSymbol(), stock2, 3L))
        testDriver.advanceWallClockTime(20L)

        val output1 = readOutput()
        val output2 = readOutput()

        assertNotNull(output1)
        assertNotNull(output2)


        assertEquals("BTC-AUD", output1.key().getSymbol())
        assertEquals(1572220800000L, output1.key().getWindowEnd())
        assertEquals(1, output1.value().getCountWindow())
        assertEquals(110.0, output2.value().getAveragePrice())
    }

    fun startProcessing(): StreamsBuilder {
        val builder = StreamsBuilder()

        val stringSerde = Serdes.StringSerde()


        fun emptyAveragePrice(): AveragePrice = AveragePrice.newBuilder()
            .setAveragePrice(0.0)
            .setSumWindow(0.0)
            .setCountWindow(0)
            .build()

        fun averagePriceAggregator(newStock: Stock, currentAveragePrice: AveragePrice): AveragePrice {
            val averagePriceBuilder: AveragePrice.Builder = AveragePrice.newBuilder(currentAveragePrice)
            // Calc Fields
            val sumWindow = currentAveragePrice.getAveragePrice() + newStock.getClose()
            val countWindow = currentAveragePrice.getCountWindow() + 1
            val calcAvgPrice = sumWindow / countWindow

            // Set Fields
            val newAveragePrice = averagePriceBuilder
                .setSumWindow(sumWindow)
                .setCountWindow(countWindow)
                .setAveragePrice(calcAvgPrice)

            // Build new AveragePrice object
            return newAveragePrice.build()
        }

        fun averagePriceWindowBuilder(symbol: String, windowEnd: Long): AveragePriceWindow {
            val avgPriceWindow = AveragePriceWindow.newBuilder()
                .setSymbol(symbol)
                .setWindowEnd(windowEnd)
                .build()
            return avgPriceWindow
        }

        val movingAvgPrice: KStream<AveragePriceWindow, AveragePrice> = builder.stream(kafkaConfigProperties.btc_event_topic, Consumed.with(
            stringSerde, stockSpecificAvroSerde,
            StockTimestampExtractor(), null))
            .groupByKey(
                Serialized.with(stringSerde, stockSpecificAvroSerde))
            .windowedBy(TimeWindows.of(Duration.ofMillis(Dimension4.day).toMillis()))
            .aggregate(
                { emptyAveragePrice() },
                { _, stc, aggregate -> averagePriceAggregator(stc, aggregate)},
                Materialized.with(stringSerde,avgPriceSpecificAvroSerde)
            )
            .toStream()
            .selectKey{ key, _ -> averagePriceWindowBuilder(key.key(), key.window().end())}

        movingAvgPrice.to(kafkaConfigProperties.avg_price_topic, Produced.with(avgPriceWindowSpecificAvroSerde, avgPriceSpecificAvroSerde))
        return builder
    }
}