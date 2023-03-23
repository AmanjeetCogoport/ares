package com.cogoport.ares.api.common.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("services")
class Services {
    var service: String? = null
    var namespace: String? = null
}
