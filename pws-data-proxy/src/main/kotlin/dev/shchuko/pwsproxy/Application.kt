package dev.shchuko.pwsproxy

import dev.shchuko.pwsproxy.impl.ktor.configureFrameworks
import dev.shchuko.pwsproxy.impl.ktor.configureRouting
import dev.shchuko.pwsproxy.impl.ktor.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    configureSerialization()
    configureFrameworks()
    configureRouting()
}
