package com.turbo.qwixxbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QwixxBackendApplication

fun main(args: Array<String>) {
    runApplication<QwixxBackendApplication>(*args)
}
