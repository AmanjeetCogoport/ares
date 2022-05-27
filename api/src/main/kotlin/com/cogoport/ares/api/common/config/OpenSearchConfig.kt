package com.cogoport.ares.api.common.config

import io.micronaut.context.annotation.ConfigurationProperties
import javax.validation.constraints.NotBlank

@ConfigurationProperties("opensearch")
data class OpenSearchConfig(
    @NotBlank
    var host: String = "localhost",

    var port: Int = 443,

    var scheme: String = "https",

    @NotBlank
    var user: String = "user",

    @NotBlank
    var pass: String = "pass"
)
