package dev.shchuko.pwsproxy.impl.ktor

import dev.shchuko.pwsproxy.api.controller.health
import dev.shchuko.pwsproxy.api.controller.restApi
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is MissingRequestParameterException -> call.respond(HttpStatusCode.BadRequest)

                is BadRequestException -> call.respond(HttpStatusCode.BadRequest)

                is NotFoundException -> call.respond(HttpStatusCode.NotFound)

                is UnsupportedMediaTypeException -> call.respond(HttpStatusCode.UnsupportedMediaType)

                is ContentTransformationException -> call.respond(HttpStatusCode.InternalServerError)

                else -> call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }

    install(AutoHeadResponse)

    routing {
        health()
        restApi()
    }
}
