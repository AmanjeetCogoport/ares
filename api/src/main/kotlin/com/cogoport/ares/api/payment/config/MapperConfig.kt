package com.cogoport.ares.api.payment.config

import com.cogoport.ares.api.payment.entity.OverallAgeingStats
import com.cogoport.ares.api.payment.mapper.*
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

    @Bean
    fun getCollectionTrendMapper(): CollectionTrendMapper {
        return Mappers.getMapper(CollectionTrendMapper::class.java)
    }

    @Bean
    fun getOverallStats(): OverallStatsMapper{
        return Mappers.getMapper(OverallStatsMapper::class.java)
    }

    @Bean
    fun getOverallAgeingStats(): OverallAgeingMapper{
        return Mappers.getMapper(OverallAgeingMapper::class.java)
    }

    @Bean
    fun getInvoiceList(): InvoiceMapper{
        return Mappers.getMapper(InvoiceMapper::class.java)
    }
}
