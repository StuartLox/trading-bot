const kafka = require('kafka-node');
require('dotenv').config();

const schemaRegistryUrl = process.env.SCHEMA_REGISTRY_URL;
const registry = require('avro-schema-registry')(schemaRegistryUrl);
var metricListner = module.exports;


const kafkaTopic = process.env.KAFKA_TOPIC;
const groupId = process.env.CONSUMER_GROUPID;
const host =  process.env.KAFKA_BOOTSTRAP_URL;

var options = {
  kafkaHost: host,
  id: groupId,
  groupId: groupId,
  fromOffset: 'earliest',
  encoding: 'buffer',
  keyEncoding: 'buffer'
  // sasl: { mechanism: "plain", username: "test", password: "test123" }
};

var subscribers = [];

metricListner.subscribeTometrics = function (callback) {
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