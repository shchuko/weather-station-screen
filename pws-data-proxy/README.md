# A service to cache PWS station data from WindGuru or XWeather (PWSWeather)

A note: data collection from XWeather is currently disabled, requires refactorings

1. Set environment variables:

WindGuru: https://stations.windguru.cz/json_api_stations.html
- `WIND_GURU_STATION_UID`
- `WIND_GURU_PASSWORD`

XWeather: https://www.xweather.com/docs/weather-api/endpoints/observations-archive
- `X_WEATHER_STATION_ID`
- `X_WEATHER_CLIENT_ID`
- `X_WEATHER_CLIENT_SECRET`

To disable one of the sources, don't pass respective env vars. WindGuru is a primary source.

2. Start application (it'll be available on `localhost:8080` by default)

3. Query `/app/rest/v1/pws/data` endpoint. Response example:
```json
{
  "ready": true,
  "history": [
    {
      "timestamp": "2024-08-30T03:35:00Z",
      "temperatureC": 14.8,
      "temperatureFeelsLikeC": 15.0,
      "humidityRH": 76.0,
      "windKnots": 7.0,
      "windGustKnots": 9.0,
      "windDirectionDeg": 338.0
    },
    {
      "timestamp": "2024-08-30T03:36:00Z",
      "temperatureC": 14.8,
      "temperatureFeelsLikeC": 15.0,
      "humidityRH": 76.0,
      "windKnots": 7.0,
      "windGustKnots": 8.0,
      "windDirectionDeg": 315.0
    }
  ]
}
```

Also, you can pass date in ISO8601 format in `since=` parameter: `/app/rest/v1/pws/data?since=2024-08-31T03:35:00Z`


Data is refreshed once a minute. See `dev.shchuko.pwsproxy.impl.service.PwsDataServiceImpl`