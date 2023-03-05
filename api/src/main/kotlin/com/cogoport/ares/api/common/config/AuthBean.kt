package com.cogoport.ares.api.common.config

import com.cogoport.brahma.authentication.AuthInterceptor
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory

@Factory
class AuthBean {
    @Bean
    fun authBean(): AuthInterceptor {
        return AuthInterceptor()
    }
}
