package com.stuartloxton.bitcoinprice.streams.metrics

import com.stuartloxton.bitcoinprice.BitcoinMetricEventWindow
import com.stuartloxton.bitcoinpriceadapter.Stock
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.streams.kstream.KGroupedStream
import org.apache.kafka.streams.kstream.KStream

interface Metric<T> {
    fun identity(): T
    fun aggregator(newStock: Stock, current: T): T
    fun topology(stream: KGroupedStream<String, Stock>, schemaRegistryClient: SchemaRegistryClient): KStream<BitcoinMetricEventWindow, T>
}