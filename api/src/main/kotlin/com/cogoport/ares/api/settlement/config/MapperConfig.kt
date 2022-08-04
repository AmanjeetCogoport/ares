package com.cogoport.ares.api.settlement.config

import com.cogoport.ares.api.settlement.mapper.*
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import org.mapstruct.factory.Mappers

@Factory
class MapperConfig {
    @Bean
    fun getSettlementConvert(): SettlementMapper {
        return Mappers.getMapper(SettlementMapper::class.java)
    }

    @Bean
    fun getHistoryDocumentConverter(): HistoryDocumentMapper {
        return Mappers.getMapper(HistoryDocumentMapper::class.java)
    }

    @Bean
    fun getSettledInvoiceConverter(): SettledInvoiceMapper {
        return Mappers.getMapper(SettledInvoiceMapper::class.java)
    }

    @Bean
    fun getDocumentConverter(): DocumentMapper {
        return Mappers.getMapper(DocumentMapper::class.java)
    }

    @Bean
    fun orgSummaryConverter(): OrgSummaryMapper {
        return Mappers.getMapper(OrgSummaryMapper::class.java)
    }

    @Bean
    fun tdsDocumentConverter(): InvoiceDocumentMapper {
        return Mappers.getMapper(InvoiceDocumentMapper::class.java)
    }

    @Bean
    fun JournalVoucherMapper(): JournalVoucherMapper {
        return Mappers.getMapper(JournalVoucherMapper::class.java)
    }
}
