package dev.shchuko.weather.station.screen.model

import kotlinx.datetime.Instant

data class PwsStateWrapper(
    val updatedAt: Instant,
    val measurementData: MeasurementData,
    val windHistory: List<WindDataPoint>,
    val windGustHistory: List<WindDataPoint>,
)