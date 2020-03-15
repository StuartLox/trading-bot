// const kafka = require('kafka-node');
// const schemaRegister = require('avro-schema-registry');
// const avroSchema = require('./models/avroSchema')
// let kafkaServiceInstance = null;

// class KafkaService {

//     constructor() {
//         this.client = new kafka.KafkaClient({ kafkaHost: 'kafka:9092' })
//         this.schemaRegistry = schemaRegister('http://schemaregistry:8081');
//         this.producer = new kafka.Producer(this.client)
//         this.producer.on('ready', () => {
//             console.log('Kafka Producer is connected and ready.')
//             this.isReady = true
//         })
//         this.isReady = false

//         this.producer.on('error', (error) => {
//             console.error(error)
//         })
//     }

//     sleep(ms) {
//         return new Promise(resolve => setTimeout(resolve, ms))
//     }

//     async sendRecord(topic, record, schema, keySchema, callback) {
//         let retries = 0

//         while (!this.isReady && retries < 3) {
//             retries += 1
//             await this.sleep(100)
//         }

//         if (!this.isReady) {
//             console.log('Kafka producer is not ready.  Try again later.')
//             return false
//         }

//         this.schemaRegistry.encodeKey(topic, keySchema, record.id)
//             .then((keyMsg) => {
//                 this.schemaRegistry.encodeMessage(topic, schema, record)
//                     .then((msg) => {
//                         const payloads = [{
//                             topic: topic,
//                             key: keyMsg,
//                             messages: msg,
//                         }]
//                         this.producer.send(payloads, callback)
//                     })
//             })
//         return true
//     }
// }

// function getKafkaServiceInstance() {
//     if (!kafkaServiceInstance) {
//         kafkaServiceInstance = new KafkaService()
//     }
//     return kafkaServiceInstance;
// }


// const kafkaConn = new KafkaService()

// var keySchema = { "type": "string" }
// var record = { "id": "1234" };

  
// kafkaConn.sendRecord('test-node.v6', record, avroSchema, keySchema, (err, data) => {
//     if (err) {
//         console.log(err);
//     }
//     console.log(data);
// });