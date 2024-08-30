package dev.shchuko.pwsproxy.api.service.model

class PwsData(
    history: List<PwsMeasurement>?,
) {
    val ready: Boolean = history != null
    val history: List<PwsMeasurement>? = history?.sortedBy { it.timestamp }

    override fun toString(): String {
        return "PwsData(ready=$ready, history=${history?.size})"
    }
}