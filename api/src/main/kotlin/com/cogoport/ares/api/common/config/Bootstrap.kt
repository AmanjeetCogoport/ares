package com.cogoport.loki.api.config

import com.cogoport.brahma.opensearch.Client
import com.cogoport.brahma.opensearch.Configuration
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.logging.Logger

@Singleton
class Bootstrap {

    @Inject
    private lateinit var openSearchConfig: OpenSearchConfig

    @EventListener
    fun onStartupEvent(event: ServerStartupEvent) {
        // Add all bootstrap services here

        Logger.getLogger("Boostrap").info("OpenSearch configured with params from application props")
        configureOpenSearch()
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
}
