package com.tyczj.signalserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SignalserverApplication{
    @Bean
    fun signalingWebSocketHandler(): WebSocketHandler {
        return WebSocketHandler()
    }
}

fun main(args: Array<String>) {
    runApplication<SignalserverApplication>(*args)
}
