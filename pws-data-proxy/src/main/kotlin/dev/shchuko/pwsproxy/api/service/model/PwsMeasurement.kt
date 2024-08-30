package dev.shchuko.pwsproxy.api.service.model

import kotlinx.datetime.Instant

data class PwsMeasurement(
    val timestamp: Instant,
    val temperatureC: Double?,
    val rh: Double?,
    val windKnots: Double?,
    val windGustKnots: Double?,
    val windDirectionDeg: Double?,
    val temperatureFeelsLikeC: Double?,
)