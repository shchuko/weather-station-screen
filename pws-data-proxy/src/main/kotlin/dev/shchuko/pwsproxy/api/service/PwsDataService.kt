package dev.shchuko.pwsproxy.api.service

import dev.shchuko.pwsproxy.api.service.model.PwsData
import kotlinx.datetime.Instant

interface PwsDataService {
    fun getData(since: Instant? = null): PwsData
}