// Handle REST requests (POST and GET) for departments
var express = require('express') //npm install express
  , bodyParser = require('body-parser') // npm install body-parser
  , fs = require('fs')
  , https = require('https')
  , http = require('http')
  , request = require('request');

var stockListener = require("./stockListener.js");
var sseMW = require('./sse');

var PORT = 8123;

const app = express()
  .use(bodyParser.urlencoded({ extended: true }))
  //configure sseMW.sseMiddleware as function to get a stab at incoming requests, in this case by adding a Connection property to the request
  .use(sseMW.sseMiddleware)
  // .use(express.static(__dirname + '/public'))
  .get('/updates', function (req, res) {
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

var stockCache = {};
stockListener.subscribeTostocks((message) => {

  var stockEvent = JSON.parse(message);
  console.log(stockEvent)
  stockCache[stockEvent.symbol] = stockEvent;
  var newData = JSON.stringify({symbol: "BTC", timestamp: stockEvent.windowEnd, averagePrice: stockEvent.averagePrice})
  var eventString = `event: priceStateUpdate\ndata: ${newData}\n\n`;
  updateSseClients(eventString);
})