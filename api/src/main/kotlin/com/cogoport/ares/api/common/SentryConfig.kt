package com.cogoport.ares.api.common
import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("sentry")
data class SentryConfig(
    var dsn: String = "https",
)
