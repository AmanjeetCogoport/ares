package com.cogoport.ares.api.common.config.model

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("services")
class Services {
    public var service: String? = null
}
