package com.stuartloxton.bitcoinprice.serdes

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.common.serialization.Deserializer

class GenericAvroDeserializer<T> : Deserializer<T> {

    internal var inner: KafkaAvroDeserializer

    /**
     * Constructor used by Kafka Streams.
     */
    constructor() {
        inner = KafkaAvroDeserializer()
    }

    constructor(client: SchemaRegistryClient) {
        inner = KafkaAvroDeserializer(client)
    }

    constructor(client: SchemaRegistryClient, props: Map<String, *>) {
        inner = KafkaAvroDeserializer(client, props)
    }

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        inner.configure(configs, isKey)
    }

    override fun deserialize(s: String, bytes: ByteArray): T {
        return inner.deserialize(s, bytes) as T
    }

    override fun close() {
        inner.close()
    }
}