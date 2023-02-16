package com.cogoport.ares.api.common.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("services.auth")
class AuthConfig {
    var alternateRoute: Boolean? = false
    var authDisabled: Boolean? = false
    var microserviceAuthToken: String? = null
}
