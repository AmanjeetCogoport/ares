package com.cogoport.ares.api.common.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("services")
class Services {
    var staging: String? = null
    var service: List<ServerObject?>? = null
    var namespace: String? = null
}
