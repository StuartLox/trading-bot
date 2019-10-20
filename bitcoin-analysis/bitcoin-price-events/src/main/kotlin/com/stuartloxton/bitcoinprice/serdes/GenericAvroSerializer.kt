package com.stuartloxton.bitcoinprice.serdes

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.common.serialization.Serializer

class GenericAvroSerializer<T> : Serializer<T> {

    internal var inner: KafkaAvroSerializer

    /**
     * Constructor used by Kafka Streams.
     */
    constructor() {
        inner = KafkaAvroSerializer()
    }

    constructor(client: SchemaRegistryClient) {
        inner = KafkaAvroSerializer(client)
    }

    override fun configure(configs: Map<String, *>, isKey: Boolean) {
        inner.configure(configs, isKey)
    }

    override fun serialize(topic: String, record: T): ByteArray {
        return inner.serialize(topic, record)
    }

    override fun close() {
        inner.close()
    }
}