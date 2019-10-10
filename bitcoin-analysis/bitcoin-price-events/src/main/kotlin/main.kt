

import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.File
import java.util.*

private fun createProducer(brokers: String, schemaRegistryUrl: String): Producer<String, GenericRecord> {
    val props = Properties()
    props["bootstrap.servers"] = brokers
    props["key.serializer"] = KafkaAvroSerializer::class.java
    props["value.serializer"] = KafkaAvroSerializer::class.java
    props["schema.registry.url"] = schemaRegistryUrl
    return KafkaProducer<String, GenericRecord>(props)
}

data class Person(
    val firstName: String,
    val lastName: String,
    val birthDate: String
)

val fakePerson = Person("Stuart", "Loxton", "2018-02-21")

val schema = Schema.Parser().parse(File("src/main/avro/person.avsc"))

val avroPerson = GenericRecordBuilder(schema).apply {
    set("firstName", fakePerson.firstName)
    set("lastName", fakePerson.lastName)
    set("birthDate", fakePerson.birthDate)
}.build()

fun main(){
    val producer = createProducer("localhost:9092", "http://localhost:8081")
    val futureResult = producer.send(ProducerRecord("personTopic1", fakePerson.firstName, avroPerson))
    val waitTimeBetweenIterationsMs = 1000L / 3
    Thread.sleep(waitTimeBetweenIterationsMs)

//    val x = URL("https://google.com").readText()
}
