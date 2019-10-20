package com.stuartloxton.bitcoinprice.serdes

import com.stuartloxton.bitcoinprice.Stock
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.processor.TimestampExtractor

// Extracts the embedded timestamp of a record (giving you "event-time" semantics).
class StockTimestampExtractor : TimestampExtractor {

    override fun extract(record: ConsumerRecord<Any, Any>, previousTimestamp: Long): Long {
        // `Foo` is your own custom class, which we assume has a method that returns
        // the embedded timestamp (milliseconds since midnight, January 1, 1970 UTC).
        var timestamp: Long = -1
        val event = record.value() as Stock
        if (event != null) {
            timestamp = event.getTimestamp() * 1000
        }
        return if (timestamp < 0) {
            // Invalid timestamp!  Attempt to estimate a new timestamp,
            // otherwise fall back to wall-clock time (processing-time).
            if (previousTimestamp >= 0) {
                previousTimestamp
            } else {
                System.currentTimeMillis()
            }
        } else timestamp
    }

}