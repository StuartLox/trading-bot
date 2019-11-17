var KafkaAvro = require('kafka-node-avro');
const Settings  = {
  "kafka" : {
    "kafkaHost" : "localhost:9092"
  },
  "schema": {
    "registry" : "http://localhost:8081"
  }
};

KafkaAvro.init(Settings).then( kafka => {
    var consumer = kafka.addConsumer("node-test.v1");
    consumer.on('message', function (message) {
        console.log(message);
    })
} , error => {
  // something wrong happen
});
