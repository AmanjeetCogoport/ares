package com.cogoport.ares.api.payment.config

import com.cogoport.ares.api.payment.mapper.AccUtilizationToPaymentMapper
import com.cogoport.ares.api.payment.mapper.AccountUtilizationMapper
import com.cogoport.ares.api.payment.mapper.CollectionTrendMapper
import com.cogoport.ares.api.payment.mapper.DailyOutstandingMapper
import com.cogoport.ares.api.payment.mapper.InvoiceMapper
import com.cogoport.ares.api.payment.mapper.OrgOutstandingMapper
import com.cogoport.ares.api.payment.mapper.OutstandingAgeingMapper
import com.cogoport.ares.api.payment.mapper.OutstandingMapper
import com.cogoport.ares.api.payment.mapper.OverallAgeingMapper
import com.cogoport.ares.api.payment.mapper.OverallStatsMapper
import com.cogoport.ares.api.payment.mapper.PayableFileToAccountUtilMapper
import com.cogoport.ares.api.payment.mapper.PayableFileToPaymentMapper
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
    fun getAccountUtilizationConverter(): AccountUtilizationMapper {
        return Mappers.getMapper(AccountUtilizationMapper::class.java)
    }

    @Bean
    fun getDsoConverter(): DailyOutstandingMapper {
        return Mappers.getMapper(DailyOutstandingMapper::class.java)
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
    fun getOverallStats(): OverallStatsMapper {
        return Mappers.getMapper(OverallStatsMapper::class.java)
    }

    @Bean
    fun getOverallAgeingStats(): OverallAgeingMapper {
        return Mappers.getMapper(OverallAgeingMapper::class.java)
    }

    @Bean
    fun getInvoiceList(): InvoiceMapper {
        return Mappers.getMapper(InvoiceMapper::class.java)
    }
    @Bean
    fun getOutstandingAgeing(): OutstandingAgeingMapper {
        return Mappers.getMapper(OutstandingAgeingMapper::class.java)
    }
    @Bean
    fun getOrgOutstanding(): OrgOutstandingMapper {
        return Mappers.getMapper(OrgOutstandingMapper::class.java)
    }

    @Bean
    fun payableFileToPayment(): PayableFileToPaymentMapper {
        return Mappers.getMapper(PayableFileToPaymentMapper::class.java)
    }
    @Bean
    fun payableFileToAccountUtilization(): PayableFileToAccountUtilMapper {
        return Mappers.getMapper(PayableFileToAccountUtilMapper::class.java)
    }

    @Bean
    fun getAccUtilizationConverter(): AccUtilizationToPaymentMapper {
        return Mappers.getMapper(AccUtilizationToPaymentMapper::class.java)
    }
}
