package com.cogoport.ares.api.settlement.config

import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
import com.cogoport.ares.api.settlement.mapper.OrgSummaryMapper
import com.cogoport.ares.api.settlement.mapper.SettledInvoiceMapper
import com.cogoport.ares.api.settlement.mapper.SettlementMapper
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
}
