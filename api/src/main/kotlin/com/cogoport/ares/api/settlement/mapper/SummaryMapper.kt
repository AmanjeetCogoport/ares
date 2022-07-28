package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.SummaryResponse
import org.mapstruct.Mapper

@Mapper
interface SummaryMapper {
    fun convertToModel(summaryResponse: SummaryResponse): com.cogoport.ares.model.settlement.SummaryResponse
}
