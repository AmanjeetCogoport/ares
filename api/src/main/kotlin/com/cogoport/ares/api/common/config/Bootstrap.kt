package com.cogoport.ares.api.common.config

import com.cogoport.ares.api.common.models.SageConfig
import com.cogoport.ares.api.utils.logger
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.opensearch.Configuration
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.sentry.Sentry
import io.sentry.SentryOptions
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class Bootstrap {

    @Inject private lateinit var openSearchConfig: OpenSearchConfig

    @Inject private lateinit var sentryConfig: SentryConfig

    @Inject private lateinit var sageConfig: SageConfig

    @EventListener
    fun onStartupEvent(@Suppress("UNUSED_PARAMETER") event: ServerStartupEvent) {
        // Add all bootstrap services here
        logger().info("Log from normal bootstrapper")
        configureOpenSearch()
        configureSentry()
        configureSage()
    }

    private fun configureOpenSearch() {
        // Configure and start OpenSearch Client
        Client.configure(
            configuration =
            Configuration(
                scheme = openSearchConfig.scheme,
                host = openSearchConfig.host,
                port = openSearchConfig.port,
                user = openSearchConfig.user,
                pass = openSearchConfig.pass
            )
        )
    }

    private fun configureSentry() {
        if (sentryConfig.enabled == true) {
            Sentry.init { options: SentryOptions ->
                options.setDsn(sentryConfig.dsn)
                options.setTracesSampleRate(1.0)
                options.setDebug(false)
            }
        }
    }

    private fun configureSage() {
        com.cogoport.brahma.sage.Client.configure(
            com.cogoport.brahma.sage.Configuration(
                soapUrl = sageConfig.soapUrl!!,
                user = sageConfig.user!!,
                password = sageConfig.password!!,
                queryClientUrl = sageConfig.queryClientUrl!!,
                queryClientPassword = sageConfig.queryClientPassword!!,
                loginUrl = sageConfig.loginUrl!!,
                username = sageConfig.username!!,
                userpassword = sageConfig.userpassword!!,
                sageRestToken = null
            )
        )
    }
}
