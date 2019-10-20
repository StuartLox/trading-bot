package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.AveragePrice
import com.stuartloxton.bitcoinprice.Stock
import com.stuartloxton.bitcoinprice.serdes.StockTimestampExtractor
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.TimeWindows
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*


@Component
class Streams {

    private val stockSpecificAvroSerde = SpecificAvroSerde<Stock>()
    private val avgPriceSpecificAvroSerde = SpecificAvroSerde<AveragePrice>()

    @Bean("kafkaStreamProcessing")
    fun startProcessing(@Qualifier("app1StreamBuilder")  builder: StreamsBuilder): KStream<String, AveragePrice> {

        stockSpecificAvroSerde.configure(Collections.singletonMap(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081"), false)
        avgPriceSpecificAvroSerde.configure(Collections.singletonMap(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081"),false)

        val longSerde = Serdes.LongSerde()
        val stringSerde = Serdes.StringSerde()
        val doubleSerde = Serdes.DoubleSerde()

        fun emptyAveragePrice(): AveragePrice = AveragePrice.newBuilder().setAveragePrice(0.0).build()

        val btcAvroStream = builder.stream("bitcoin-price-aud.v8", Consumed.with(
            stringSerde,stockSpecificAvroSerde,
            StockTimestampExtractor(), null))
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMillis(Dimension4.day).toMillis()))
            .aggregate(
                { emptyAveragePrice() },
                { _, stc, aggregate -> AveragePrice.newBuilder().setAveragePrice(aggregate.getAveragePrice() + stc.getClose()).build() }
            )
            .toStream()
            .selectKey{key,_ -> key.key()}

        btcAvroStream.to("another-topic-3", Produced.with(stringSerde, avgPriceSpecificAvroSerde))

        return btcAvroStream
    }
}