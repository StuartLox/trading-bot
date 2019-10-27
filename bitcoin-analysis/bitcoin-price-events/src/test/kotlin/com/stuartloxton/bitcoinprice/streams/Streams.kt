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
import junit.framework.Assert.assertNotNull
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.kafka.test.rule.EmbeddedKafkaRule
import org.springframework.test.context.junit4.SpringRunner
import java.util.*




@SpringBootTest
@RunWith(SpringRunner::class)
class StreamTest {
    companion object {
        @ClassRule
        @JvmField
        val embeddedKafka = EmbeddedKafkaRule(1, false, "demo-topic")
    }


    @MockBean
    private lateinit var kafkaConfigProperties: KafkaConfig

    @MockBean
    private lateinit var stockEventProducer: StockEventProducer

    private lateinit var testDriver: TopologyTestDriver


    val schemaRegistryClient: SchemaRegistryClient = MockSchemaRegistryClient()

    val stringSerde = Serdes.StringSerde()

    val INPUT_TOPIC = "TestTopicInput"
    val OUTPUT_TOPIC = "TestTopicOutput"
    private var schemaRegistryUrl = "http://dummy"
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

        val defaultSerdeConfig = Collections.singletonMap(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)

        stockSpecificAvroSerde.configure(defaultSerdeConfig, false)
        avgPriceSpecificAvroSerde.configure(defaultSerdeConfig,false)
        avgPriceWindowSpecificAvroSerde.configure(defaultSerdeConfig,true)

        val builder = StreamsBuilder()
        val input = builder.stream(INPUT_TOPIC, Consumed.with(stringSerde, stockSpecificAvroSerde))
        val config = getStreamsConfiguration()
        testDriver = TopologyTestDriver(builder.build(), config)

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
        val stock = Stock(
            "BTC-AUD",1572177505L,
            100.0,100.0,100.0,
            100.0,100.0
        )
        val props = getStreamsConfiguration()
        val stockSerializer = stockSpecificAvroSerde.serializer()

        val factory = ConsumerRecordFactory<String, Stock>(StringSerializer(), stockSerializer)
        val record = factory.create(INPUT_TOPIC, stock.getSymbol(), stock)
        testDriver.pipeInput(record)

    }
}