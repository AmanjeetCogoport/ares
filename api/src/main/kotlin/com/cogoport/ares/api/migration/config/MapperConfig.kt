package com.cogoport.ares.api.migration.config

import com.cogoport.ares.api.migration.mapper.PaymentMapper
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import org.mapstruct.factory.Mappers

@Factory
class MapperConfig {

    @Bean
    fun getMigrationPaymentMapping(): PaymentMapper {
        return Mappers.getMapper(PaymentMapper::class.java)
    }
}
