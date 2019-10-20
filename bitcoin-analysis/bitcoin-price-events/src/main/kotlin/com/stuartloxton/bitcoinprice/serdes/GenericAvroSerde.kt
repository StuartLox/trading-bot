package com.stuartloxton.bitcoinprice.serdes

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer

class GenericAvroSerde<T> : Serde<T> {

    private val inner: Serde<T>

    /**
     * Constructor used by Kafka Streams.
     */
    constructor() {
        inner = Serdes.serdeFrom(GenericAvroSerializer(), GenericAvroDeserializer())
    }

    @JvmOverloads constructor(client: SchemaRegistryClient, props: Map<String, *> = emptyMap<String, Any>()) {
        inner = Serdes.serdeFrom(GenericAvroSerializer(client), GenericAvroDeserializer(client, props))
    }

    override fun serializer(): Serializer<T> {
        return inner.serializer()
    }

    override fun deserializer(): Deserializer<T> {
        return inner.deserializer()
    }

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        inner.serializer().configure(configs, isKey)
        inner.deserializer().configure(configs, isKey)
    }

    override fun close() {
        inner.serializer().close()
        inner.deserializer().close()
    }

}