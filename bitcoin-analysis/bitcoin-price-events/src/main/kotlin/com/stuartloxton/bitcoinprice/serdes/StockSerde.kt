package com.stuartloxton.bitcoinprice.serdes

import com.stuartloxton.bitcoinprice.Stock
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class StockSerde : Serde<Stock> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
    override fun deserializer(): Deserializer<Stock> = StockDeserializer()
    override fun serializer(): Serializer<Stock> = StockSerializer()
}

class StockSerializer : Serializer<Stock> {
    override fun serialize(topic: String, data: Stock?): ByteArray? {
        if (data == null) return null
        return jsonMapper.writeValueAsBytes(data)
    }

    override fun close() {}
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
}

class StockDeserializer : Deserializer<Stock> {
    override fun deserialize(topic: String, data: ByteArray?): Stock? {
        if (data == null) return null
        return jsonMapper.readValue(data, Stock::class.java)
    }

    override fun close() {}
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
}