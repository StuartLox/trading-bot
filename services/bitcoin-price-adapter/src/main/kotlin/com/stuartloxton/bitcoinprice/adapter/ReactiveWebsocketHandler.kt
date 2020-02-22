package com.stuartloxton.bitcoinprice.adapter

import com.stuartloxton.kotlinwebsocket.Stock
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import java.net.URI

@Component
class ReactiveWebSocketHandler(private val stockEventProducer: StockEventProducer): CommandLineRunner {
    @Value("\${application.btc-stream-uri}")
    private val btcUri = ""
    private val logger: Logger = LoggerFactory.getLogger(ReactiveWebSocketHandler::class.java)

    fun getBtcStock(quotes: JSONArray): Stock? {
        for (i in 0 until quotes.length()) {
            val item = quotes.getJSONObject(i)
            if (item.getString("s") == "BTCUSDT") {
                logger.debug(item.toString())
                val volume = item.getDouble("v")
                val open = item.getDouble("o")
                val high = item.getDouble("h")
                val low = item.getDouble("l")
                val close = item.getDouble("c")
                val timestamp = item.getLong("E")
                return Stock(
                        "BTC", timestamp,
                        open, high, low,
                        close, volume)
            }
        }
        return null
    }

    fun handleStreamQuotes(jsonString: String) {
        val jsonObj = JSONObject(jsonString).get("data")
        val stock = when (jsonObj) {
            is JSONArray -> getBtcStock(jsonObj)
            else -> null
        }
        if (stock != null) {
            stockEventProducer.stockEventProducer(stock)
        }
    }

    override fun run(vararg args: String?) {
        val uri = URI(btcUri)
        val client = ReactorNettyWebSocketClient()
        client.execute(uri) {session ->
            session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(::handleStreamQuotes)
                    .then()
        }.block()
    }
}
