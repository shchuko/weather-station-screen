package dev.shchuko.pwsproxy.api.controller

import dev.shchuko.pwsproxy.api.controller.dto.PwsDataDto
import dev.shchuko.pwsproxy.api.controller.dto.PwsMeasurementDto
import dev.shchuko.pwsproxy.api.service.model.PwsData
import dev.shchuko.pwsproxy.api.service.model.PwsMeasurement

fun PwsMeasurement.toDto() = PwsMeasurementDto(
    timestamp = timestamp,
    temperatureC = temperatureC,
    temperatureFeelsLikeC = temperatureFeelsLikeC,
    humidityRH = rh,
    windKnots = windKnots,
    windGustKnots = windGustKnots,
    windDirectionDeg = windDirectionDeg,
)

fun PwsData.toDto() = PwsDataDto(
    ready = ready,
    history = history?.map(PwsMeasurement::toDto),
)
