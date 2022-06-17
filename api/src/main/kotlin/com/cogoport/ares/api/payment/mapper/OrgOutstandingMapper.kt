package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.OrgOutstanding
import com.cogoport.ares.model.payment.OrgOutstandingResponse
import org.mapstruct.Mapper

@Mapper
interface OrgOutstandingMapper {
    fun convertToModel(orgOutstanding: OrgOutstanding): OrgOutstandingResponse

    fun convertToEntity(orgOutstanding: OrgOutstandingResponse): OrgOutstanding
}
