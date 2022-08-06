package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.OrgOutstanding
import com.cogoport.ares.model.payment.response.OrgOutstandingResponse
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface OrgOutstandingMapper {
    fun convertToModel(orgOutstanding: OrgOutstanding): OrgOutstandingResponse

    fun convertToEntity(orgOutstanding: OrgOutstandingResponse): OrgOutstanding
}
