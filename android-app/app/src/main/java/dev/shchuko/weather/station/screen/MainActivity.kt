package dev.shchuko.weather.station.screen

import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.axis.rememberAxisTickComponent
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.chart.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.chart.zoom.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.color
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.theme.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.theme.VicoTheme
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.chart.values.AxisValueOverrider
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.model.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.model.lineSeries
import com.patrykandpatrick.vico.core.scroll.Scroll
import com.patrykandpatrick.vico.core.zoom.Zoom
import dev.shchuko.weather.station.screen.dto.PwsDataDto
import dev.shchuko.weather.station.screen.dto.PwsMeasurementDto
import dev.shchuko.weather.station.screen.model.MeasurementData
import dev.shchuko.weather.station.screen.model.PwsMeasurementHistoryPoint
import dev.shchuko.weather.station.screen.model.PwsStateWrapper
import dev.shchuko.weather.station.screen.model.WindDataPoint
import dev.shchuko.weather.station.screen.model.toPwsMeasurementHistoryPoint
import dev.shchuko.weather.station.screen.ui.theme.WeatherStationScreen
import dev.shchuko.weather.station.screen.ui.theme.textDp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import java.lang.Exception
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val weatherViewModel: WeatherViewModel = viewModel()

            WeatherStationScreen {
                WeatherScreen(weatherViewModel)
            }
        }
    }
}


class WeatherViewModel : ViewModel() {
    var pwsState by mutableStateOf<PwsStateWrapper?>(null)
        private set

    init {
        fetchAndUpdateWeatherData()
    }

    private fun fetchAndUpdateWeatherData() {
        viewModelScope.launch {
            while (isActive) {
                pwsState = fetchWeatherData()
                delay(1.minutes) // Fetch data every 30 seconds
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeatherScreen(viewModel: WeatherViewModel) {
    val pwsState = viewModel.pwsState
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windGraphModelProducer = remember { CartesianChartModelProducer.build() }

    LaunchedEffect(Unit) {
        clearGraph(windGraphModelProducer)
        while (isActive) {
            updateGraphModel(windGraphModelProducer, viewModel.pwsState)
            delay(5.seconds) // 30 seconds delay
        }
    }

    Scaffold(
        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp),
                        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                    ) {
                        MyTextClock()
                        Text("Armenian Camp", style = TextStyle(fontSize = 25.textDp))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(2.dp, 2.dp, 2.dp, 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "github.com/shchuko/weather-station-screen",
                        style = TextStyle(fontSize = 16.textDp)
                    )
                    Text("Updated ${pwsState?.measurementData?.lastMeasurementTime?.let {updatedAt -> Clock.System.now().minus(updatedAt).inWholeMinutes} ?: "--"} min ago", style = TextStyle(fontSize = 16.textDp))
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints {
            val boxWidth = maxWidth

            val fontSize = 50.textDp
            val fontSize2 = 25.textDp
            val fontSize3 = 23.textDp
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(4f)
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    val columnWidth = boxWidth / 3
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(columnWidth)
                            .padding(start = 30.dp)
                    ) {
                        Text(text = "Wind ${pwsState?.measurementData?.windKnots.toWindString()} kts", style = TextStyle(fontSize = fontSize))
                        Text(text = "(${pwsState?.measurementData?.windMps.toWindString()} m/s)", style = TextStyle(fontSize = fontSize2))
                    }
                    Column(
                        modifier = Modifier
                            .width(columnWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = pwsState?.measurementData?.windDirectionDeg.toWindDirectionString(), style = TextStyle(fontSize = fontSize))
                        Text(text = "(${pwsState?.measurementData?.windDirectionDeg.toWindString()}\u00B0)", style = TextStyle(fontSize = fontSize2))
                    }
                    Column(
                        modifier = Modifier
                            .width(columnWidth)
                            .padding(end = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = "Gust ${pwsState?.measurementData?.windGustKnots.toWindString()} kts", style = TextStyle(fontSize = fontSize))
                        Text(text = "(${pwsState?.measurementData?.windGustMps.toWindString()} m/s)", style = TextStyle(fontSize = fontSize2))
                    }
                }

                WindHistoryChart(
                    modelProducer = windGraphModelProducer,
                    hoursDepth = 6,
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(7f)
                        .fillMaxWidth()
                        .fillMaxHeight()
                )

                Row(
                    horizontalArrangement = Arrangement.Absolute.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .fillMaxHeight()
                        .padding(bottom = 8.dp)
                ) {
                    Text(text = "Temp: ${pwsState?.measurementData?.temperatureC.toTemperatureString()}", style = TextStyle(fontSize = fontSize3))
                    Text(text = "Rh: ${pwsState?.measurementData?.humidityRH.toHumidityString()}", style = TextStyle(fontSize = fontSize3))
                    Text(text = "Feels like ${pwsState?.measurementData?.temperatureFeelsLikeC.toTemperatureString()}", style = TextStyle(fontSize = fontSize3))
                }
            }
        }
    }
}

fun Double?.toWindString(): String = this?.roundToInt()?.toString() ?: "--"
fun Double?.toHumidityString(): String = when {
    this == null -> "--"
    else -> "${this.roundToInt()}"
} + "%"

fun Double?.toTemperatureString(): String = when {
    this == null -> "--"
    this > 0 -> "+${this.roundToInt()}"
    this < 0 -> "-${this.absoluteValue.roundToInt()}"
    else -> "0"
} + "\u00B0C"
@Composable
fun WindHistoryChart(
    modelProducer: CartesianChartModelProducer,
    hoursDepth: Int,
    modifier: Modifier = Modifier,
) {
    val windLineColor = Color(0xffa485e0)
    val gustLineColor = Color(0x81A485E0)

    class DefaultColors(
        val cartesianLayerColors: List<Long>,
        val elevationOverlayColor: Long,
        val lineColor: Long,
        val textColor: Long,
    )

    val Dark = DefaultColors(
        cartesianLayerColors = listOf(0xffcacaca, 0xffa8a8a8, 0xff888888),
        elevationOverlayColor = 0xffffffff,
        lineColor = 0xff555555,
        textColor = 0xffffffff,
    )

    val Light: DefaultColors =
        DefaultColors(
            cartesianLayerColors = listOf(0xff787878, 0xff5a5a5a, 0xff383838),
            elevationOverlayColor = 0x00000000,
            lineColor = 0x47000000,
            textColor = 0xde000000,
        )

    fun fromDefaultColors1(defaultColors: DefaultColors) =
        VicoTheme(
            defaultColors.cartesianLayerColors.map(::Color),
            Color(defaultColors.elevationOverlayColor),
            Color(defaultColors.lineColor),
            Color(defaultColors.textColor),
        )

    ProvideVicoTheme(fromDefaultColors1(Dark)) {
        CartesianChartHost(
            rememberCartesianChart(
                rememberLineCartesianLayer(
                    lines = rememberLineSpec(
                        DynamicShaders.color(windLineColor),
                        backgroundShader = null
                    ).wrapWithList(),
                    axisValueOverrider = AxisValueOverrider.fixed(
                        minX = -hoursDepth * 3600f,
                        maxX = 0f,
                        minY = 0f,
                    ),
                ),
                rememberLineCartesianLayer(
                    lines = rememberLineSpec(
                        shader = DynamicShaders.color(gustLineColor),
                        backgroundShader = null
                    ).wrapWithList()
                ),

                startAxis = rememberStartAxis(
                    axis = rememberAxisLineComponent(color = Color(0xff555555)),
                    tick = rememberAxisTickComponent(
                        color = Color(0xff555555),
                        dynamicShader = null
                    ),
                    guideline = rememberAxisGuidelineComponent(color = Color(0xff555555)),
                    title = "knots",
                    itemPlacer = AxisItemPlacer.Vertical.step({ 5f }, shiftTopLines = true),
                    titleComponent = rememberTextComponent(
                        color = Color.Black,
                        background = rememberShapeComponent(Shapes.pillShape, windLineColor),
                        padding = dimensionsOf(horizontal = 8.dp, vertical = 2.dp),
                        margins = dimensionsOf(end = 4.dp),
                        typeface = Typeface.MONOSPACE,
                    ),
                ),
                bottomAxis = rememberBottomAxis(
                    axis = rememberAxisLineComponent(color = Color(0xff555555)),
                    tick = rememberAxisTickComponent(
                        color = Color(0xff555555),
                        dynamicShader = null
                    ),
                    guideline = null,
                    itemPlacer = remember { SameDistanceItemPlacer(6) },
                    valueFormatter = { x, _, _ ->
                        val hours = -(x / 3600).roundToInt()
                        if (hours == 1) "$hours hour ago" else "$hours hours ago"
                    },
                    title = "wind speed",
                    titleComponent = rememberTextComponent(
                        color = Color.Black,
                        background = rememberShapeComponent(Shapes.pillShape, windLineColor),
                        padding = dimensionsOf(horizontal = 8.dp, vertical = 2.dp),
                        margins = dimensionsOf(end = 4.dp),
                        typeface = Typeface.MONOSPACE,
                    ),
                ),
            ),
            modelProducer = modelProducer,
            modifier = modifier.padding(end = 16.dp),
            scrollState = rememberVicoScrollState(
                initialScroll = Scroll.Absolute.End,
            ),
            zoomState = rememberVicoZoomState(initialZoom = Zoom.Content),
            runInitialAnimation = false,
            diffAnimationSpec = null
        )
    }
}

class SameDistanceItemPlacer(
    private val segmentsNumber: Int,
) : AxisItemPlacer.Horizontal by AxisItemPlacer.Horizontal.default() {
    override fun getLabelValues(
        context: ChartDrawContext,
        visibleXRange: ClosedFloatingPointRange<Float>,
        fullXRange: ClosedFloatingPointRange<Float>,
        maxLabelWidth: Float
    ): List<Float> {
        val segmentWidth = (context.chartValues.maxX - context.chartValues.minX) / segmentsNumber
        return (1 until segmentsNumber).map { segmentNumber ->
            context.chartValues.minX + segmentNumber * segmentWidth
        }
    }

    override fun getLineValues(
        context: ChartDrawContext,
        visibleXRange: ClosedFloatingPointRange<Float>,
        fullXRange: ClosedFloatingPointRange<Float>,
        maxLabelWidth: Float
    ): List<Float>? = null
}

private fun <T> T.wrapWithList() = listOf(this)


@Volatile
var history: List<PwsMeasurementHistoryPoint>? = null

suspend fun fetchWeatherData(): PwsStateWrapper {
    val response: List<PwsMeasurementDto>? = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }.use { client ->
        try {
            val response =
                client.get("https://weather.armeniancamp.shchuko.dev/app/rest/v1/pws/data")
            if (response.status.isSuccess()) {
                response.body<PwsDataDto>().takeIf { it.ready }?.history
            } else {
                null
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    val now = Clock.System.now()
    val minAllowedHistoryTimestamp = now.minus(6.hours)
    val minAllowedDashboardTimestamp = now.minus(10.minutes)

    history = (response?.map { it.toPwsMeasurementHistoryPoint() } ?: history)
        ?.filter { it.timestamp > minAllowedHistoryTimestamp }
        ?.sortedBy { it.timestamp }


    return PwsStateWrapper(
        updatedAt = now,
        measurementData = MeasurementData(
            temperatureC = history?.lastOrNull { it.temperatureC != null }
                ?.takeIf { it.timestamp >= minAllowedDashboardTimestamp }?.temperatureC,
            temperatureFeelsLikeC = history?.lastOrNull { it.temperatureFeelsLikeC != null }
                ?.takeIf { it.timestamp >= minAllowedDashboardTimestamp }?.temperatureFeelsLikeC,
            humidityRH = history?.lastOrNull { it.humidityRH != null }
                ?.takeIf { it.timestamp >= minAllowedDashboardTimestamp }?.humidityRH,
            windKnots = history?.lastOrNull { it.windKnots != null }
                ?.takeIf { it.timestamp >= minAllowedDashboardTimestamp }?.windKnots,
            windGustKnots = history?.lastOrNull { it.windGustKnots != null }
                ?.takeIf { it.timestamp >= minAllowedDashboardTimestamp }?.windGustKnots,
            windDirectionDeg = history?.lastOrNull { it.windDirectionDeg != null }
                ?.takeIf { it.timestamp >= minAllowedDashboardTimestamp }?.windDirectionDeg,
            lastMeasurementTime = history?.maxOfOrNull { it.timestamp }
        ),
        windHistory = history?.mapNotNull {
            it.windKnots?.let { w -> WindDataPoint(it.timestamp, w) }
        } ?: emptyList(),
        windGustHistory = history?.mapNotNull {
            it.windGustKnots?.let { w -> WindDataPoint(it.timestamp, w) }
        } ?: emptyList(),
    )
}

fun clearGraph(modelProducer: CartesianChartModelProducer) {
    modelProducer.tryRunTransaction {
        lineSeries {
            series(listOf(0, -10), listOf(10, 10))
        }
    }
}
fun updateGraphModel(modelProducer: CartesianChartModelProducer, state: PwsStateWrapper?) {
    if (state == null || state.windGustHistory.isEmpty() && state.windHistory.isEmpty()) {
        clearGraph(modelProducer)
        return
    }

    modelProducer.tryRunTransaction {
        windLineSeries(state.updatedAt, state.windHistory)
        windLineSeries(state.updatedAt, state.windGustHistory)
    }
}

fun CartesianChartModelProducer.Transaction.windLineSeries(now: Instant, wind: List<WindDataPoint>) {
    lineSeries {
        var next = mutableListOf<WindDataPoint>()
        wind.forEach { point ->
            if (next.isEmpty() || point.timestamp - next.last().timestamp <= 4.minutes) {
                next += point
            } else {
                series(
                    x = next.map { (it.timestamp - now).inWholeSeconds },
                    y = next.map { it.knots },
                )
                next = mutableListOf()
            }
        }
        series(
            x = next.map { (it.timestamp - now).inWholeSeconds },
            y = next.map { it.knots },
        )
    }
}


@Composable
fun MyTextClock() {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(1000L) // Update every second
        }
    }

    Text(text = currentTime, style = TextStyle(fontSize = 25.textDp))
}

fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

fun Double?.toWindDirectionString(): String {
    if (this == null) return "--"

    val directions = arrayOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    )

    val index = ((this / 22.5) + 0.5).toInt() % 16
    return directions[index]
}