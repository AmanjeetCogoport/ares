package com.cogoport.ares.api.payment.config

import com.cogoport.ares.api.payment.mapper.DsoMapper
import com.cogoport.ares.api.payment.mapper.OutstandingMapper
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
    fun getDsoConverter(): DsoMapper {
        return Mappers.getMapper(DsoMapper::class.java)
    }

    @Bean
    fun getOutstandingConverter(): OutstandingMapper {
        return Mappers.getMapper(OutstandingMapper::class.java)
    }
}
