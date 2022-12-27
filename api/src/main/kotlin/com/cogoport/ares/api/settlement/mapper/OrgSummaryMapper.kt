package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.payment.entity.OrgSummary
import com.cogoport.ares.model.settlement.OrgSummaryResponse
import org.mapstruct.Mapper
import org.mapstruct.MapperConfig
import org.mapstruct.ReportingPolicy

@Mapper
@MapperConfig(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface OrgSummaryMapper {
    fun convertToModel(orgSummary: OrgSummary): OrgSummaryResponse

    fun convertToEntity(orgSummaryResponse: OrgSummaryResponse): OrgSummary
}
