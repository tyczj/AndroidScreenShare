package com.tyczj.signalserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SignalserverApplication

fun main(args: Array<String>) {
    runApplication<SignalserverApplication>(*args)
}
