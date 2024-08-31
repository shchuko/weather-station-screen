package dev.shchuko.weather.station.screen.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PwsMeasurementDto(
    val timestamp: Instant,
    val temperatureC: Double?,
    val temperatureFeelsLikeC: Double?,
    val humidityRH: Double?,
    val windKnots: Double?,
    val windGustKnots: Double?,
    val windDirectionDeg: Double?,
)