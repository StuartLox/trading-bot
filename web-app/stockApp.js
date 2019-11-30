// Handle REST requests (POST and GET) for departments
var express = require('express') //npm install express
  , bodyParser = require('body-parser') // npm install body-parser
  , fs = require('fs')
  , https = require('https')
  , http = require('http')
  , request = require('request');

var logger = require("./logger.js");
var stockListener = require("./stockListener.js");
var sseMW = require('./sse');

var PORT = 8123;

const app = express()
  .use(bodyParser.urlencoded({ extended: true }))
  //configure sseMW.sseMiddleware as function to get a stab at incoming requests, in this case by adding a Connection property to the request
  .use(sseMW.sseMiddleware)
  .use(express.static(__dirname + '/stock_public'))
  .get('/updates', function (req, res) {
    console.log("res (should have sseConnection)= " + res.sseConnection);
    var sseConnection = res.sseConnection;
    console.log("sseConnection= ");
    sseConnection.setup();
    sseClients.add(sseConnection);
  });

const server = http.createServer(app);

// const WebSocket = require('ws');
// // create WebSocket Server
// const wss = new WebSocket.Server({ server });
// wss.on('connection', (ws) => {
//   console.log('WebSocket Client connected');
//   ws.on('close', () => console.log('Client disconnected'));

//   ws.on('message', function incoming(message) {
//     // if (message.indexOf("tweetLike") > -1) {
//     //   var tweetLike = JSON.parse(message);
//     //   var likedTweet = tweetCache[tweetLike.tweetId];
//     //   if (likedTweet) {
//     //     updateWSClients(JSON.stringify({ "eventType": "tweetLiked", "likedTweet": likedTweet }));
//     //     tweetLikeProducer.produceTweetLike(likedTweet);
//     //   }
//     // }
//   });
// });

server.listen(PORT, function listening() {
  console.log('Listening on %d', server.address().port);
});
// setInterval(() => {
//   updateWSClients(JSON.stringify({ "eventType": "time", "time": new Date().toTimeString() }));
// }, 1000);

// function updateWSClients(message) {
//   wss.clients.forEach((client) => {
//     client.send(message);
//   });

// }

// Realtime updates
var sseClients = new sseMW.Topic();

updateSseClients = function (message) {
  sseClients.forEach(function (sseConnection) {
    console.log('workin\n\n');
    sseConnection.send(message);
    console.log(message);
  }
    , this // this second argument to forEach is the thisArg (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/forEach) 
  );
}

console.log('server running on port 3000');

// heartbeat
// setInterval(() => {
//   updateSseClients({ "eventType": "stockEvent", "symbol": "BTC-AUD" , "volume": "10000", "high": "200", "low": "200", "close": "200", "timestamp": "2019-10-10T22:11:11Z", "open": "200" })
// }
//   , 10000
// )
var stockCache = {};
stockListener.subscribeTostocks((message) => {

  var stockEvent = JSON.parse(message);
  console.log(stockEvent)
  stockCache[stockEvent.symbol] = stockEvent;
  updateSseClients(stockEvent);
})

// tweetAnalyticsListener.subscribeToStockAnalytics((message) => {
//   console.log("stock analytic " + message);
//   var stockAnalyticsEvent = JSON.parse(message);
//   console.log("tweetAnalyticsEvent " + JSON.stringify(stockAnalyticsEvent));
//   updateSseClients(stockAnalyticsEvent);
// })

// tweetLikesAnalyticsListener.subscribeToTweetLikeAnalytics((message) => {
//   console.log("tweetLikes analytic " + message);
//   var tweetLikesAnalyticsEvent = JSON.parse(message);
//   //{"nrs":[{"tweetId":"1495112906610001DCWw","conference":"oow17","count":27,"window":null},{"tweetId":"1492900954165001X6eF","conference":"oow17","count":22,"window":null},{"tweetId":"1496421364049001nhas","conference":"oow17","count":19,"window":null},null]}
//   //tweetLikes analytic {"nrs":[{"tweetId":"1495112906610001DCWw","conference":"oow17","count":27,"window":null},{"tweetId":"1492900954165001X6eF","conference":"oow17","count":22,"window":null},{"tweetId":"1496421364049001nhas","conference":"oow17","count":19,"window":null},null]}
//   // enrich tweetLikesAnalytic - add tweet text and author
//   for (var i = 0; i < 3; i++) {
//     if (tweetLikesAnalyticsEvent.nrs[i]) {
//       // get tweet from local cache
//       var tweetId = tweetLikesAnalyticsEvent.nrs[i].tweetId;
//       console.log("tweet id = " + tweetId);
//       var tweet = tweetCache[tweetId];
//       if (tweet) {
//         tweetLikesAnalyticsEvent.nrs[i].text = tweet.text;
//         tweetLikesAnalyticsEvent.nrs[i].author = tweet.author;
//       }
//     }
//   }

//   tweetLikesAnalyticsEvent.eventType = "tweetLikesAnalytics";
//   tweetLikesAnalyticsEvent.conference = tweetLikesAnalyticsEvent.nrs[0].conference;
//   console.log("tweetLikesAnalyticsEvent " + JSON.stringify(tweetLikesAnalyticsEvent));
//   updateSseClients(tweetLikesAnalyticsEvent);
// })