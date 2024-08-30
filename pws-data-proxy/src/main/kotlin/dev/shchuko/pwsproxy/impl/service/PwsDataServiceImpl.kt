package dev.shchuko.pwsproxy.impl.service

import dev.shchuko.pwsproxy.api.service.PeriodicJob
import dev.shchuko.pwsproxy.api.service.PwsDataService
import dev.shchuko.pwsproxy.api.service.model.PwsData
import dev.shchuko.pwsproxy.api.service.model.PwsMeasurement
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.lang.Math.pow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class PwsDataServiceImpl(
    private val serviceConfig: PwsProxyServiceConfig,
) : PwsDataService, PeriodicJob {
    companion object {
        private val REFRESH_INTERVAL = 1.minutes
        private const val MEASUREMENT_INTETVAL_MINUTES = 1
        private val MEASUREMENTS_KEEP_DISTACE = 6.hours

        private val logger = LoggerFactory.getLogger(PwsDataServiceImpl::class.java)
    }

    @Suppress("SpellCheckingInspection", "PropertyName")
    @Serializable
    private data class WindGuruWeatherData(
        val unixtime: List<Long>,
        val wind_avg: List<Double?>,
        val wind_max: List<Double?>,
        val wind_direction: List<Double?>,
        val temperature: List<Double?>,
        val rh: List<Double?>
    ) {
        fun toPwsMeasurements(): List<PwsMeasurement> {
            logger.debug("Mapping WindGuru reponse into PwsMeasurement list, measurements={}", unixtime.size)

            val notSortedResult = ArrayList<PwsMeasurement>(unixtime.size)
            for (i in unixtime.indices) {
                notSortedResult.add(
                    PwsMeasurement(
                        timestamp = Instant.fromEpochSeconds(unixtime[i]),
                        temperatureC = temperature[i],
                        rh = rh[i],
                        windKnots = wind_avg[i],
                        windGustKnots = wind_max[i],
                        windDirectionDeg = wind_direction[i],
                        temperatureFeelsLikeC = calculateFeelsLikeTemperature(temperature[i], wind_avg[i], rh[i])
                    )
                )
            }
            val result = notSortedResult.sortedBy { it.timestamp }
            logger.debug("Mapped WingGuru reponse into PwsMeasurement list, measurements={}", result.size)
            return result
        }
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var measurementHistory: List<PwsMeasurement>? = null
    private var lastRefreshTime: Instant? = null
    private var refreshJobInitialized = AtomicBoolean(false)

    override fun getData(since: Instant?): PwsData {
        val data =
            if (since == null) measurementHistory
            else measurementHistory?.filter { it.timestamp >= since }

        logger.info("Data requested since={} resultLength={}", since, data?.size)
        return PwsData(data)
    }

    override fun launch() {
        if (!refreshJobInitialized.compareAndSet(false, true)) {
            logger.info("Refresh job is already launched, skipping")
            return
        }

        logger.info("Launching refresh job")

        coroutineScope.launch {
            while (isActive) {
                val latestKnownMeasurementTimestamp =
                    measurementHistory?.maxOf { it.timestamp } ?: Instant.DISTANT_PAST


                val currentTime = Clock.System.now()
                val measurementKeepTimestamp = currentTime - MEASUREMENTS_KEEP_DISTACE

                // load measurements since last refresh or since max reasonable distance in the past from now
                val loadDataSince = lastRefreshTime ?: measurementKeepTimestamp

                logger.info(
                    "Performing refresh currentTime={} latestKnownMeasurementTimestamp={} measurementKeepTimestamp={} loadDataSince={}",
                    currentTime, latestKnownMeasurementTimestamp, measurementKeepTimestamp, loadDataSince
                )

                // try loading from WindGuru, and then from PWSWeather
                val newMeasurements = loadWindGuruData(since = loadDataSince)
                        ?.filter { it.timestamp > latestKnownMeasurementTimestamp }

                if (newMeasurements != null) {
                    // if new data received, merge it with history
                    measurementHistory = ((measurementHistory ?: emptyList()) + newMeasurements)
                        .filter { it.timestamp >= measurementKeepTimestamp }
                    lastRefreshTime = currentTime
                    logger.info("Updated data measurementHistory={} lastRefreshTime={}", measurementHistory?.size, lastRefreshTime)
                } else {
                    // on error, adjust the lastRefreshTime time if needed to avoid loading too old data
                    lastRefreshTime = maxOf(
                        a = lastRefreshTime ?: measurementKeepTimestamp,
                        b = measurementKeepTimestamp,
                    )
                    logger.info("Updated data lastRefreshTime={}", lastRefreshTime)
                }

                delay(REFRESH_INTERVAL)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    private suspend fun loadWindGuruData(since: Instant): List<PwsMeasurement>? {
        try {
            logger.info("Loading WindGuru data since={}", since)
            val config = serviceConfig.windGuruConfig ?: let {
                logger.info("WindGuru config not found, skipping")
                return null
            }

            val response = httpClient.get {
                url("https://www.windguru.cz/int/wgsapi.php")
                parameter("uid", config.windGuruStationUid)
                parameter("password", config.windGuruPassword)
                parameter("q", "station_data")
                parameter("from", since)
                parameter("avg_minutes", "$MEASUREMENT_INTETVAL_MINUTES")
                parameter(
                    "vars",
                    listOf(
                        "unixtime",
                        "wind_avg",
                        "wind_max",
                        "wind_direction",
                        "temperature",
                        "rh",
                    ).joinToString(separator = ","),
                )
            }

            return if (response.status.isSuccess()) {
                logger.info("WindGuru responded with http success, parsing response code={}", response.status.value)
                val body = response.body<WindGuruWeatherData>()
                logger.info("Parsed WindGuru response, loaded entries={}", body.unixtime.size)
                body.toPwsMeasurements()
            } else {
                logger.warn("WindGuru responded with http error, code={}", response.status.value)
                null
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            logger.error("WindGuru request failed with exception message={}", e.message, e)
            return null
        }
    }
}

private fun calculateFeelsLikeTemperature(
    temperatureCelsius: Double?,
    windSpeedKnots: Double?,
    humidityPercent: Double?,
): Double? {
    temperatureCelsius ?: return null
    windSpeedKnots ?: return null
    humidityPercent ?: return null

    // Convert wind speed from knots to kilometers per hour
    val windSpeedKph = windSpeedKnots * 1.852
    return if (temperatureCelsius <= 10 && windSpeedKph > 4.8) {
        // Wind Chill Index formula
        13.12 + 0.6215 * temperatureCelsius -
                11.37 * windSpeedKph.pow(0.16) +
                0.3965 * temperatureCelsius * windSpeedKph.pow(0.16)
    } else if (temperatureCelsius >= 27 && humidityPercent >= 40) {
        // Convert temperature to Fahrenheit for Heat Index formula
        val temperatureFahrenheit = temperatureCelsius * 9 / 5 + 32
        // Heat Index formula
        -42.379 +
                2.04901523 * temperatureFahrenheit +
                10.14333127 * humidityPercent -
                0.22475541 * temperatureFahrenheit * humidityPercent -
                0.00683783 * temperatureFahrenheit.pow(2.0) -
                0.05481717 * humidityPercent.pow(2.0) +
                0.00122874 * temperatureFahrenheit.pow(2.0) * humidityPercent +
                0.00085282 * temperatureFahrenheit * humidityPercent.pow(2.0) -
                0.00000199 * temperatureFahrenheit.pow(2.0) * pow(humidityPercent, 2.0)
    } else {
        // If neither condition applies, return the actual temperature
        temperatureCelsius
    }
}
