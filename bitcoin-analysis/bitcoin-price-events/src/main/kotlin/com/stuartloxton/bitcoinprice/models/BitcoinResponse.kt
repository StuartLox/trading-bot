package com.stuartloxton.bitcoinprice.models

import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.TopicProcessor

data class Event(val sender: Int, val value: Int)

@Component
class PrimeNumbersHandler : WebSocketHandler {
    private val processor = TopicProcessor.share<Event>("shared", 1024)

    override fun handle(session: WebSocketSession): Mono<Void> {
        return session.send(
            processor
                .map { ev -> session.textMessage("${ev.sender}:${ev.value}") }
        ).and(
            session.receive()
                .map { ev ->
                    val parts = ev.payloadAsText.split(":")
                    Event(sender = parts[0].toInt(), value = parts[1].toInt())
                }
                .filter { ev -> isPrime(ev.value) }
                .log()
                .doOnNext { ev -> processor.onNext(ev) }
        )
    }
    private fun isPrime(num: Int): Boolean {
        if (num < 2) return false
        if (num == 2) return true
        if (num % 2 == 0) return false
        var i = 3
        while (i * i <= num) {
            if (num % i == 0) return false
            i += 2
        }
        return true
    }

}
