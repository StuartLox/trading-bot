package com.stuartloxton.bitcoinprice.handler


import com.stuartloxton.bitcoinprice.Stock
import com.stuartloxton.bitcoinprice.models.StockResponse
import com.stuartloxton.bitcoinprice.streams.StockEventProducer
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@Component
class StockHandler(private val stockEventProducer: StockEventProducer) {
    private var symbol: String = ""
    private val log: Logger = LoggerFactory.getLogger(StockHandler::class.java)

    fun sendGet(symbol: String): String {
        this.symbol = symbol
        val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?symbol=$symbol")
        log.info("Get Request")
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET

            val text = inputStream.bufferedReader().readText()
            inputStream.close()
            return text
        }
    }

    fun handleQuotes(jsonString: String): ArrayList<Stock> {
        val jsonArray = JSONObject(jsonString)

        val jsonObj = jsonArray
            .getJSONObject("chart")
            .getJSONArray("result")
            .getJSONObject(0)

        val quoteJson = jsonObj
            .getJSONObject("indicators")
            .getJSONArray("quote")
            .getJSONObject(0)

        val volume = quoteJson.getJSONArray("volume")
        val open = quoteJson.getJSONArray("open")
        val high = quoteJson.getJSONArray("high")
        val low = quoteJson.getJSONArray("low")
        val close = quoteJson.getJSONArray("close")
        val timestamp = jsonObj.getJSONArray("timestamp")

        val list = ArrayList<Stock>()

        var i = 0
        while (i < timestamp.length()) {
            try {
                val s = Stock(
                    this.symbol,
                    timestamp.getLong(i),
                    open.getDouble(i),
                    high.getDouble(i),
                    low.getDouble(i),
                    close.getDouble(i),
                    volume.getDouble(i)
                )
                list.add(s)
                log.info(s.toString())
                val x = stockEventProducer.stockEventProducer(s)
                log.info(x.toString())
            } catch(e: Exception) {
                log.error(open[i].toString())
                log.error(volume[i].toString())
                log.error(timestamp[i].toString())
            }
            i++
        }
        return list
    }


    fun handleStocks(symbol: String): Unit {
        val json = sendGet(symbol)
        log.info(json)
        val quotes = handleQuotes(json)
        quotes.forEach {
            publishStockEvents(it)
        }
    }

    fun handleStreamQuotes(jsonString: String){
        val jsonObj = JSONObject(jsonString)
        val quoteJson = jsonObj
            .getJSONArray("data")
            .getJSONObject(3)

        val volume = quoteJson.getDouble("volume")
        val open = quoteJson.getDouble("open")
        val high = quoteJson.getDouble("high")
        val low = quoteJson.getDouble("low")
        val close = quoteJson.getDouble("close")
        val timestamp = jsonObj.getLong("timestamp")
        val stock = Stock(
            this.symbol, timestamp,
            open, high, low,
            close, volume
        )
        stockEventProducer.stockEventProducer(stock)
    }

    fun handleStock(request: ServerRequest): Mono<ServerResponse> {
        log.info("Start of StockHandler | handleStock")
        val s = handleStocks(request.pathVariable("symbol"))
        log.info(s.toString())
        val resp = StockResponse("", mutableListOf(), "Data")
        val body = request.bodyToMono(Stock::class.java).flatMap { Mono.just(resp) }
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(body, StockResponse::class.java)
    }

    fun publishStockEvents(stock: Stock) {
        val success = stockEventProducer.stockEventProducer(stock)
        log.debug(success.toString())
    }

    fun startStream(request: ServerRequest): Mono<ServerResponse> {
        log.info("Start of StockHandler | startStream")
        val s = handleStreamQuotes(request.pathVariable("symbol"))
        val resp = StockResponse("", mutableListOf(), "Data")
        val body = request.bodyToMono(Stock::class.java).flatMap { Mono.just(resp) }
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(body, StockResponse::class.java)
    }
}