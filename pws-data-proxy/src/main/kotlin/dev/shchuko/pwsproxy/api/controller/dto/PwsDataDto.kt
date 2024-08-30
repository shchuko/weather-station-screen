package dev.shchuko.pwsproxy.api.controller.dto

import kotlinx.serialization.Serializable

@Serializable
data class PwsDataDto(
    val ready: Boolean,
    val history: List<PwsMeasurementDto>?,
)