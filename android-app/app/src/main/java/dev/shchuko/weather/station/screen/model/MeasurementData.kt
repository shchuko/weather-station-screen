package dev.shchuko.weather.station.screen.model

import kotlinx.datetime.Instant

data class MeasurementData(
    val temperatureC: Double?,
    val temperatureFeelsLikeC: Double?,
    val humidityRH: Double?,
    val windKnots: Double?,
    val windGustKnots: Double?,
    val windDirectionDeg: Double?,
    val windMps: Double? = windKnots?.times(0.51444444444444),
    val windGustMps: Double? = windGustKnots?.times(0.51444444444444),
    val lastMeasurementTime: Instant?,
)

