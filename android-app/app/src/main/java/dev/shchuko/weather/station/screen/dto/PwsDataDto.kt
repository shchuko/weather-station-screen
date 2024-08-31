package dev.shchuko.weather.station.screen.dto

import kotlinx.serialization.Serializable

@Serializable
data class PwsDataDto(
    val ready: Boolean,
    val history: List<PwsMeasurementDto>?,
)