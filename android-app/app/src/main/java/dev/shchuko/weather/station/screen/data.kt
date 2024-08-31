package dev.shchuko.weather.station.screen

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Foo(val values: List<RootElement>)

@Serializable
data class RootElement(val ob: WeatherData)

@Serializable
data class WeatherData(
    val timestamp: Long,
    val dateTimeISO: String?,
    val tempC: Double?,
    val feelslikeC: Double?,
    val humidity: Int?,
    val windSpeedKPH: Double?,
    val windDirDEG: Int?,
    val windGustKPH: Double?,
)

private val json = Json { ignoreUnknownKeys = true }

val jsonData = json.decodeFromString<Foo>(jsonString)