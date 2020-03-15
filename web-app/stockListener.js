const kafka = require('kafka-node');
const dotenv = require('dotenv');
dotenv.config();
console.log(`Your port is ${process.env.SCHEMA_REGISTRY_URL}`);
const avroSchemaRegistry = require('avro-schema-registry');
var stockListener = module.exports;


const kafkaTopic = 'avg-bitcoin-price-topic.v1';//'kafka.test';
const host =  'localhost:9092';
const schemaRegistry = 'http://localhost:8081';

const registry = avroSchemaRegistry(schemaRegistry);

var options = {
  kafkaHost: host,
  id: 'consumer2',
  groupId: 'consumer.adapter.raw',
  fetchMaxBytes: 1024 * 1024,
  encoding: 'buffer',
  fromOffset: 'earliest',
  // sasl: { mechanism: "plain", username: "test", password: "test123" }
};

var subscribers = [];

stockListener.subscribeTostocks = function (callback) {
  subscribers.push(callback);
}

var consumerGroup = new kafka.ConsumerGroup(options, kafkaTopic);

consumerGroup.on('message', onMessage)
consumerGroup.on('error', onError)

function onMessage(rawMessage) {
  console.log('Topic="%s" Partition=%s Offset=%d', rawMessage.topic, rawMessage.partition, rawMessage.offset);
  registry.decode(rawMessage.key).then((key) => {
    registry.decode(rawMessage.value)
    .then((value) => {
      subscribers.forEach((subscriber) => {
        var keyValue = Object.assign(key, value)
        var json_str = JSON.stringify(keyValue);
        subscriber(json_str);
      })
    })
  }).catch(err => err)
}

function onError(error) {
  console.error(error);
  console.error(error.stack);
}

process.once('SIGINT', function () {
  async.each([consumerGroup], function (consumer, callback) {
    consumer.close(true, callback);
  });
});