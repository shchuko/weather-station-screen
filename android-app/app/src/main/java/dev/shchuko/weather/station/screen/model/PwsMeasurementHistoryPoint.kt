package dev.shchuko.weather.station.screen.model

import kotlinx.datetime.Instant

data class PwsMeasurementHistoryPoint(
    val timestamp: Instant,
    val temperatureC: Double?,
    val temperatureFeelsLikeC: Double?,
    val humidityRH: Double?,
    val windKnots: Double?,
    val windGustKnots: Double?,
    val windDirectionDeg: Double?,
)

