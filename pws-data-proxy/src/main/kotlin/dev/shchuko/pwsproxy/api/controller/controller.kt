package dev.shchuko.pwsproxy.api.controller

import dev.shchuko.pwsproxy.api.service.PwsDataService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

fun Route.health() {
    get("/health") {
        call.respond(HttpStatusCode.OK, "OK")
    }
}

fun Route.restApi() {
    route("/app/rest/v1/pws/data") {
        val pwsDataService: PwsDataService by inject()

        get {
            val since = call.request.queryParameters.getInstantParam("since")
            val data = pwsDataService.getData(since)
            val remapped = data.toDto()
            call.respond(remapped)
        }
    }
}

private fun Parameters.getInstantParam(name: String): Instant? = try {
    get(name)?.let(Instant::parse)
} catch (e: IllegalArgumentException) {
    throw BadRequestException("Invalid parameter $name: ${e.message}")
}
