package com.stuartloxton.bitcoinprice.serdes

import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory

class StockSerde : Serde<Stocks> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
    override fun deserializer(): Deserializer<Stocks> = StockDeserializer()
    override fun serializer(): Serializer<Stocks> = StockSerializer()
}

class StockSerializer : Serializer<Stocks> {
    override fun serialize(topic: String, data: Stocks?): ByteArray? {
        if (data == null) return null
        return jsonMapper.writeValueAsBytes(data)
    }

    override fun close() {}
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
}

class StockDeserializer : Deserializer<Stocks> {
    private val log = LoggerFactory.getLogger(StockDeserializer::class.java)
    override fun deserialize(topic: String, data: ByteArray?): Stocks? {
        if (data == null) return null
        val jm = jsonMapper.readValue(data, Stocks::class.java)
        log.info(jm.toString() + "BLAHBLAH\n\n")
        return jm
    }

    override fun close() {}
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
}