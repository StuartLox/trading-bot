package com.stuartloxton.binance.gateway
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
class ReactiveWebSocketHandler(private val cryptoQuoteProcessor: CryptoQuoteProcessor): ApplicationRunner {
    @Value("\${application.binance-uri}")
    private val binanceUri = ""
    private val logger: Logger = LoggerFactory.getLogger(ReactiveWebSocketHandler::class.java)

    fun getCryptoQuoteEvent(item: JSONObject): Quote {
        val symbol = item.getString("s")
        val volume = item.getDouble("v")
        val open = item.getDouble("o")
        val high = item.getDouble("h")
        val low = item.getDouble("l")
        val close = item.getDouble("c")
        val timestamp = item.getLong("E")

        return Quote(
            symbol,
            timestamp,
            open,
            high,
            low,
            close,
            volume
        )
    }

    fun handleStreamQuotes(jsonString: String) {
        JSONObject(jsonString).optJSONArray("data")?.let {
            for (i in 0 until it.length()) {
                val quote = it.getJSONObject(i)
                val cryptoQuoteEvent = getCryptoQuoteEvent(quote)
                cryptoQuoteProcessor.cryptoQuoteProcessor(cryptoQuoteEvent)
            }
        }
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
