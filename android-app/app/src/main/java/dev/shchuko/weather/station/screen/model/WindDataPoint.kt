package dev.shchuko.weather.station.screen.model

import kotlinx.datetime.Instant

data class WindDataPoint(
    val timestamp: Instant,
    val knots: Double,
    val mps: Double = knots.times(0.5144444444),
)