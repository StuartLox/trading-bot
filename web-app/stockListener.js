const kafka = require('kafka-node');
const avroSchemaRegistry = require('avro-schema-registry');
var stockListener = module.exports;


const kafkaTopic = 'avg-bitcoin-price-topic.v1';//'kafka.test';
const host =  'localhost:9092';
const schemaRegistry = 'http://localhost:8081';

const Client = kafka.KafkaClient;
const registry = avroSchemaRegistry(schemaRegistry);

var client = new Client(host);


var options = {
  kafkaHost: host,
  id: 'consumer1',
  groupId: 'trader.v1.8',
  fetchMaxBytes: 1024 * 1024,
  encoding: 'buffer',
  fromOffset: 'earliest'
};


var subscribers = [];

stockListener.subscribeTostocks = function (callback) {
  subscribers.push(callback);
}

var consumerGroup = new kafka.ConsumerGroup(options, kafkaTopic);

consumerGroup.on('message', onMessage)
consumerGroup.on('error', onError)

function onMessage(rawMessage) {
  console.log('%s read msg Topic="%s" Partition=%s Offset=%d', client.clientId, rawMessage.topic, rawMessage.partition, rawMessage.offset);
  registry.decode(rawMessage.value)
    .then((msg) => {
      subscribers.forEach((subscriber) => {
        var json_str = JSON.stringify(msg);
        subscriber(json_str);
      })
    })
    .catch(err => err)
}

// setInterval(() => {
//   var msg = { 
//     "symbol": "BTC-AUD" ,
//     "volume": "10000",
//     "high": "200", "low": "200", "close": "200", "timestamp": "2019-10-10T22:11:11Z", "open": "200" };

//   var tle = JSON.stringify(msg);
//   subscribers.forEach((subscriber) => {
//     subscriber(msg);
//     console.log(tle);

//   })
// }
//   , 1000
// )

function onError(error) {
  console.error(error);
  console.error(error.stack);
}

process.once('SIGINT', function () {
  async.each([consumerGroup], function (consumer, callback) {
    consumer.close(true, callback);
  });
});