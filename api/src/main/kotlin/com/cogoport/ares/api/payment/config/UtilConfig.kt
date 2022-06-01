package com.cogoport.ares.api.payment.config

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import java.text.DateFormat
import java.text.SimpleDateFormat

@Factory
class UtilConfig {

    @Bean
    fun getDateFormatter(): DateFormat {
        return SimpleDateFormat("yyyy-mm-dd")
    }
}
