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
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.TimeWindows
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.kafka.test.rule.EmbeddedKafkaRule
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
class StreamTest {
    companion object {
        @ClassRule
        @JvmField
        val embeddedKafka = EmbeddedKafkaRule(1, false, "btc-event-topic.v1")
    }


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
    private var schemaRegistryUrl = "http://localhost:8081"

    private var stockSpecificAvroSerde = SpecificAvroSerde<Stock>(schemaRegistryClient)
    private var avgPriceSpecificAvroSerde = SpecificAvroSerde<AveragePrice>(schemaRegistryClient)
    private var avgPriceWindowSpecificAvroSerde = SpecificAvroSerde<AveragePriceWindow>(schemaRegistryClient)


    fun getStreamsConfiguration():  Properties {
        val config = Properties()
        // Need to be set even these do not matter with TopologyTestDriver
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "TopologyTestDriver")
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy.kafka.confluent.cloud:9092")
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String()::class.java)
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde::class.java)
        config.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, StockTimestampExtractor::class.java)
        config.put("schema.registry.url",schemaRegistryUrl)
        return config
    }


    @Before
    fun setup() {
        schemaRegistryClient.register(kafkaConfigProperties.btc_event_topic, Stock.`SCHEMA$`)
        schemaRegistryClient.register(kafkaConfigProperties.avg_price_topic, AveragePriceWindow.`SCHEMA$`)
        schemaRegistryClient.register(kafkaConfigProperties.avg_price_topic, AveragePrice.`SCHEMA$`)
        val defaultSerdeConfig = Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)

        stockSpecificAvroSerde.configure(defaultSerdeConfig, false)
        avgPriceSpecificAvroSerde.configure(defaultSerdeConfig,false)
        avgPriceWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)

//        val builder = StreamsBuilder()
//
//        val input = builder.stream(kafkaConfigProperties.btc_event_topic, Consumed.with(stringSerde, stockSpecificAvroSerde))
////            .to(kafkaConfigProperties.avg_price_topic, Produced.with(stringSerde, stockSpecificAvroSerde))

        val builder = startProcessing()


//        stream.to(kafkaConfigProperties.btc_event_topic, Produced.with(avgPriceWindowSpecificAvroSerde, avgPriceSpecificAvroSerde))
        val config = getStreamsConfiguration()
        testDriver = TopologyTestDriver(builder.build(), config)
    }

    fun readOutput(): ProducerRecord<AveragePriceWindow, AveragePrice> {
        val output = testDriver.readOutput(kafkaConfigProperties.avg_price_topic, avgPriceWindowSpecificAvroSerde.deserializer(), avgPriceSpecificAvroSerde.deserializer())
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

        val factory = ConsumerRecordFactory<String, Stock>(StringSerializer(), stockSerializer)
        val record1 = factory.create(kafkaConfigProperties.btc_event_topic, stock1.getSymbol(), stock1 )
        val record2 = factory.create(kafkaConfigProperties.btc_event_topic, stock2.getSymbol(), stock2)

        testDriver.pipeInput(factory.create(kafkaConfigProperties.btc_event_topic, stock1.getSymbol(), stock1, 1L ))
        testDriver.pipeInput(factory.create(kafkaConfigProperties.btc_event_topic, stock2.getSymbol(), stock2, 1L))
        testDriver.pipeInput(factory.create(kafkaConfigProperties.btc_event_topic, stock2.getSymbol(), stock2, 1L))
        val output1 = readOutput()
        val output2 = readOutput()
        val output3 = readOutput()

        assertNotNull(output1)
        assertEquals("BTC-AUD", output1.key().getSymbol())
        assertEquals(1572220800000L, output1.key().getWindowEnd())
        assertEquals(1, output1.value().getCountWindow())
        assertEquals(110.0, output3.value().getAveragePrice())
    }

    fun startProcessing(): StreamsBuilder {
        val builder = StreamsBuilder()

        val defaultSerdeConfig = Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            schemaRegistryUrl)

        stockSpecificAvroSerde.configure(defaultSerdeConfig, false)
        avgPriceSpecificAvroSerde.configure(defaultSerdeConfig,false)
        avgPriceWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)

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
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMillis(Dimension4.day).toMillis()))
            .aggregate(
                { emptyAveragePrice() },
                { _, stc, aggregate -> averagePriceAggregator(stc, aggregate) }
            )
            .toStream()
            .selectKey{ key, _ -> averagePriceWindowBuilder(key.key(), key.window().end())}

        movingAvgPrice.to(kafkaConfigProperties.avg_price_topic, Produced.with(avgPriceWindowSpecificAvroSerde, avgPriceSpecificAvroSerde))
        return builder
    }
}