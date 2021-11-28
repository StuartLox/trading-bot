package com.stuartloxton.cryptoprice.adapter
import com.stuartloxton.bitcoinpriceadapter.Stock
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import java.net.URI

@Component
class ReactiveWebSocketHandler(private val stockEventProducer: StockEventProducer): ApplicationRunner {
    @Value("\${application.binance-stream}")
    private val binanceUri = ""
    private val logger: Logger = LoggerFactory.getLogger(ReactiveWebSocketHandler::class.java)

    fun getQuotes(quotes: JSONArray): Stock? {
        for (i in 0 until quotes.length()) {
            val item = quotes.getJSONObject(i)
            val symbol = item.getString("s")
            val volume = item.getDouble("v")
            val open = item.getDouble("o")
            val high = item.getDouble("h")
            val low = item.getDouble("l")
            val close = item.getDouble("c")
            val timestamp = item.getLong("E")

            return Stock(
                symbol, timestamp,
                    open, high, low,
                    close, volume)
        }

        return null
    }

    fun handleStreamQuotes(jsonString: String) {
        val jsonObj = JSONObject(jsonString).get("data")
        val stock = getQuotes(jsonObj)
        stockEventProducer.stockEventProducer(stock)
    }

    override fun run(args: ApplicationArguments) {
        val uri = URI(binanceUri)
        val client = ReactorNettyWebSocketClient()
        client.execute(uri) {session ->
            session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(::handleStreamQuotes)
                    .then()
        }.block()
    }
}
