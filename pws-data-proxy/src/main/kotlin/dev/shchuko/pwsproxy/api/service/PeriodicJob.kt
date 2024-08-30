package dev.shchuko.pwsproxy.api.service

interface PeriodicJob {
    fun launch()
    fun stop()
}