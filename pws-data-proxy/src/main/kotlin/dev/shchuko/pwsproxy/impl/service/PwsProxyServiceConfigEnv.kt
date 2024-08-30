package dev.shchuko.pwsproxy.impl.service


object PwsProxyServiceConfigEnv : PwsProxyServiceConfig {
    override val windGuruConfig: WindGuruConfig? = run {
        val stationUid = System.getenv("WIND_GURU_STATION_UID")
        val password = System.getenv("WIND_GURU_PASSWORD")

        if (stationUid.isNullOrBlank() || password.isNullOrBlank()) null
        else object : WindGuruConfig {
            override val windGuruStationUid: String = stationUid
            override val windGuruPassword: String = password
        }
    }

    override val xWeatherConfig: XWeatherConfig? = run {
        val stationId = System.getenv("X_WEATHER_STATION_ID")
        val clientId = System.getenv("X_WEATHER_CLIENT_ID")
        val clientSecret = System.getenv("X_WEATHER_CLIENT_SECRET")

        if (stationId.isNullOrBlank() || clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) null
        else object : XWeatherConfig {
            override val xWeatherStationId: String = stationId
            override val xWeatherClientId: String = clientId
            override val xWeatherClientSecret: String = clientSecret
        }
    }
}
