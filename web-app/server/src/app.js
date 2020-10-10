var express = require('express') //npm install express
  , bodyParser = require('body-parser') // npm install body-parser
  , http = require('http')

var metricListener = require("./metricListener.js");
var sseMW = require('./sse');

var PORT = process.env.SERVER_PORT;

const app = express()
  .use(bodyParser.urlencoded({ extended: true }))
  //configure sseMW.sseMiddleware as function to get a stab at incoming requests, in this case by adding a Connection property to the request
  .use(sseMW.sseMiddleware)
  .get('/events', function (req, res) {
    console.log("res (should have sseConnection)= " + res.sseConnection);
    var sseConnection = res.sseConnection;
    console.log("sseConnection= ");
    sseConnection.setup();
    sseClients.add(sseConnection);
  });

const server = http.createServer(app);

server.listen(PORT, function listening() {
  console.log('Listening on %d', server.address().port);
});

// Realtime updates
var sseClients = new sseMW.Topic();

updateSseClients = function (message) {
  sseClients.forEach(function (sseConnection) {
    sseConnection.send(message);
    console.log(message);
  }
    , this
  );
}

console.log('server running on port 3000');

var metricCache = {};
metricListener.subscribeTometrics((message) => {
  var metricEvent = JSON.parse(message);
  metricCache[metricEvent.symbol] = metricEvent;
 
  var newData = JSON.stringify({
    symbol: "BTC", timestamp: metricEvent.windowEnd, 
    averagePrice: metricEvent.avgPrice.averagePrice,
    atr:  metricEvent.atr.averageTrueRange
  })
  var eventString = `event: priceStateUpdate\ndata: ${newData}\n\n`;
  updateSseClients(eventString);
})