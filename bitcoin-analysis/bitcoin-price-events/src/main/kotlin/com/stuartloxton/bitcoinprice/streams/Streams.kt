package com.stuartloxton.bitcoinprice.streams

import com.stuartloxton.bitcoinprice.Stock
import com.stuartloxton.bitcoinprice.serdes.StockDeserializer
import com.stuartloxton.bitcoinprice.serdes.StockSerializer
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component


@Component
class Streams {
    private val avroSerde = GenericAvroSerde().apply {
        configure(mapOf(Pair("schema.registry.url", "http://localhost:8081")), false)
    }

    val stock =  Serdes.serdeFrom<Stock>(StockSerializer(), StockDeserializer())

    @Bean("kafkaStreamProcessing")
    fun startProcessing(@Qualifier("app1StreamBuilder")  builder: StreamsBuilder): KStream<String, Stock> {
        val btc = builder.stream("bitcoin-price-aud.v8", Consumed.with(Serdes.String(), stock))
        btc.peek { key, value -> println(value) }
        btc.to("some-topic-1", Produced.with(Serdes.String(), stock))
        return btc
    }
}