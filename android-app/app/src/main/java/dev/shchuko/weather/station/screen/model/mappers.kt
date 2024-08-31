package dev.shchuko.weather.station.screen.model

import dev.shchuko.weather.station.screen.dto.PwsMeasurementDto

fun PwsMeasurementDto.toPwsMeasurementHistoryPoint() = PwsMeasurementHistoryPoint(
    timestamp = timestamp,
    temperatureC = temperatureC,
    temperatureFeelsLikeC = temperatureFeelsLikeC,
    humidityRH = humidityRH,
    windKnots = windKnots,
    windGustKnots = windGustKnots,
    windDirectionDeg = windDirectionDeg,
)