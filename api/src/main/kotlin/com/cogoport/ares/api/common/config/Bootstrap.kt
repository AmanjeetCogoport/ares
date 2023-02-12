package com.cogoport.ares.api.common.config

import com.cogoport.ares.api.common.models.SageConfig
import com.cogoport.brahma.authentication.Authentication
import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.opensearch.Configuration
import io.micronaut.context.annotation.Value
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.sentry.Sentry
import io.sentry.SentryOptions
import jakarta.inject.Inject
import jakarta.inject.Singleton
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient
import com.cogoport.brahma.authentication.Configuration as AuthConfiguration

@Singleton
class Bootstrap {

    @Inject private lateinit var openSearchConfig: OpenSearchConfig

    @Inject private lateinit var sentryConfig: SentryConfig

    @Inject private lateinit var sageConfig: SageConfig

    @Inject
    private lateinit var serviceDiscoveryClient: ServiceDiscoveryClient

    @Value("\${services.namespace}")
    private lateinit var namespace: String

    @Value("\${cogoport.api_url}")
    private lateinit var cogoUrl: String

    @EventListener
    fun onStartupEvent(@Suppress("UNUSED_PARAMETER") event: ServerStartupEvent) {
        // Add all bootstrap services here
        configureOpenSearch()
        configureSentry()
        configureSage()
        configureAuth()
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
    private fun configureAuth() {
        Authentication.configure(
            AuthConfiguration(
                namespace = namespace,
                alternateRoute = true,
                cogoUrl = cogoUrl,
                enabledForAuth = false,
                serviceDiscoveryClient = serviceDiscoveryClient
            )
        )
    }
}
