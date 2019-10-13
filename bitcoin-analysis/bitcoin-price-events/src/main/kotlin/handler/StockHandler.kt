package handler

import models.Stock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class StockHandler(private val symbol: String) {

    fun sendGet(): String {
        val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?symbol=$symbol")

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
            list.add(
                Stock(
                    symbol,
                    timestamp.getLong(i),
                    open.getDouble(i),
                    high.getDouble(i),
                    low.getDouble(i),
                    close.getDouble(i),
                    volume.getDouble(i)
                )
            )
            i++
        }
        return list
    }

    fun handleStock(): ArrayList<Stock> {
        val json = streams.sendGet()
        val quotes = streams.handleQuotes(json)
        return quotes
    }
}