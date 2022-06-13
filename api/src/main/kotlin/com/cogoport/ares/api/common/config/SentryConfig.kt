package com.cogoport.ares.api.common.config

import io.micronaut.context.annotation.ConfigurationProperties
import javax.validation.constraints.NotBlank

@ConfigurationProperties("sentry")
data class SentryConfig(
    @NotBlank
    var dsn: String? = "dsn",
    var enabled: Boolean? = false
)
