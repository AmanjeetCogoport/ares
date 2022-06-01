package com.cogoport.ares.api.payment.config

import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.mapper.PaymentToPaymentMapper
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import org.mapstruct.factory.Mappers

@Factory
class MapperConfig {

    @Bean
    fun getPaymentConverter(): PaymentToPaymentMapper {
        return Mappers.getMapper(PaymentToPaymentMapper::class.java)
    }

    @Bean
    fun getAccUtilizationConverter(): AccUtilizationToPaymentMapper {
        return Mappers.getMapper(AccUtilizationToPaymentMapper::class.java)
    }
}
