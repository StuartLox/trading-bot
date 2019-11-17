const KafkaAvro = require('kafka-node-avro');
const Settings = {
    "kafka": {
        "kafkaHost": "localhost:9092"
    },
    "schema": {
        "registry": "http://localhost:8081"
    }
};
var avroSchema = {
    name: 'MyAwesomeType',
    type: 'record',
    fields: [
        {
            name: 'id',
            type: 'string'
        }, {
            name: 'timestamp',
            type: 'double'
        }, {
            name: 'enumField',
            type: {
                name: 'EnumField',
                type: 'enum',
                symbols: ['sym1', 'sym2', 'sym3']
            }
        }]
};

var keyAvroSchema = {
    name: 'MyAwesomeType',
    type: 'record',
    fields: [
        {
            name: 'id',
            type: 'string'
        }]
};
const avroSchemaRegistry = require('avro-schema-registry');
const schemaRegistry = 'http://localhost:8081';

const registry = avroSchemaRegistry(schemaRegistry);
var kafka = require('kafka-node');
var HighLevelProducer = kafka.HighLevelProducer;
var KeyedMessage = kafka.KeyedMessage;

var client = new kafka.KafkaClient({ kafkaHost: 'localhost:9092' });

// For this demo we just log client errors to the console.
client.on('error', function (error) {
    console.error(error);
});

var producer = new HighLevelProducer(client);

producer.on('ready', function () {
    // Create message and encode to Avro buffer

    var record = {
        enumField: 'sym1',
        id: '3e0c63c4-956a-4378-8a6d-2de636d191de',
        timestamp: Date.now()
    };

    topic = "node-kafka.v1"
    registry.encodeMessage(topic, avroSchema, record)
            .then((msg) => {
                const payloads = [{
                    topic: topic,
                    key: record.id,
                    messages: msg,
                }]
                this.producer.send(payloads, callback)
            })

    //Send payload to Kafka and log result/error
    // producer.send(payload, function (error, result) {
    //     console.info('Sent payload to Kafka: ', payload);
    //     if (error) {
    //         console.error(error);
    //     } else {
    //         var formattedResult = result[0];
    //         console.log('result: ', result)
    //     }
    // });
});

// For this demo we just log producer errors to the console.
producer.on('error', function (error) {
    console.error(error);
});  
