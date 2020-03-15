const kafka = require('kafka-node');
const avroSchemaRegistry = require('avro-schema-registry');
var subscribers = [];

stockListener.subscribeTostocks = function (callback) {
  subscribers.push(callback);
}

const kafkaTopic = 'bitcoin-price-aud.v1';//'kafka.test';
const host = 'localhost:9092';
const schemaRegistry = 'http://localhost:8081';

const Consumer = kafka.Consumer;
const Client = kafka.KafkaClient;
const registry = avroSchemaRegistry(schemaRegistry);

var client = new Client(host);
var topics = [{
  topic: kafkaTopic
}];

var options = {
  kafkaHost: host,
  id: 'consumer1',
  groupId: 'ExampleTestGroup.v4',
  fetchMaxBytes: 1024 * 1024,
  encoding: 'buffer',
  fromOffset: 'earliest',
  sasl: { mechanism: 'plain', username: 'test', password: 'test123' }
};

var consumerGroup = new kafka.ConsumerGroup(options, kafkaTopic);

consumerGroup.on('message', onMessage)
consumerGroup.on('error', onError)

function onMessage(rawMessage) {
  console.log('%s read msg Topic="%s" Partition=%s Offset=%d', client.clientId, rawMessage.topic, rawMessage.partition, rawMessage.offset);
  registry.decode(rawMessage.value)
    .then((msg) => {
      console.log("Message Value " + msg)
      subscribers.forEach((subscriber) => {
        subscriber(message.value);
      })
    })
    .catch(err => err)
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