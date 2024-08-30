package dev.shchuko.pwsproxy.impl.ktor

import dev.shchuko.pwsproxy.api.service.PwsDataService
import dev.shchuko.pwsproxy.impl.service.*
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()

        modules(module {
            single<PwsProxyServiceConfig> { PwsProxyServiceConfigEnv }

            single<PwsDataService>(createdAtStart = true) {
                PwsDataServiceImpl(get()).apply { launch() }
            } onClose {
                (it as PwsDataServiceImpl).stop()
            }
        })
    }
}
