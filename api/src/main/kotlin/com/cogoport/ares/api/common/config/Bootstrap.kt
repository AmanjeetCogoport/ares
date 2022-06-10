package com.cogoport.ares.api.common.config

import com.cogoport.ares.api.common.SentryConfig
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.opensearch.Configuration
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.sentry.Sentry
import io.sentry.SentryOptions
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.logging.Logger

@Singleton
class Bootstrap {

    @Inject
    private lateinit var openSearchConfig: OpenSearchConfig

    @Inject
    private lateinit var sentryConfig: SentryConfig

    @EventListener
    fun onStartupEvent(event: ServerStartupEvent) {
        // Add all bootstrap services here

        Logger.getLogger("Boostrap").info("OpenSearch configured with params from application props")
        configureOpenSearch()
        configureSentry()
    }

    private fun configureOpenSearch() {
        // Configure and start OpenSearch Client
        Client.configure(
            configuration = Configuration(
                scheme = openSearchConfig.scheme,
                host = openSearchConfig.host,
                port = openSearchConfig.port,
                user = openSearchConfig.user,
                pass = openSearchConfig.pass
            )
        )
    }

    private fun configureSentry() {
        Logger.getLogger("Boostrap").info("Sentry configured with params from application props")
        Sentry.init { options: SentryOptions ->
            options.dsn = sentryConfig.dsn
            // Set tracesSampleRate to 1.0 to capture 100% of transactions for performance monitoring.
            // We recommend adjusting this value in production.
            options.tracesSampleRate = 1.0
            // When first trying Sentry it's good to see what the SDK is doing:
            // options.setDebug(true)
        }
    }
}
