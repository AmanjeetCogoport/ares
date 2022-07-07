package com.cogoport.ares.api.settlement.config

import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
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
}
