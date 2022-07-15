package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.OrgStatsResponse
import org.mapstruct.Mapper

@Mapper
interface OrgStatsMapper {
    fun convertToModel(orgStats: OrgStatsResponse): com.cogoport.ares.model.payment.OrgStatsResponse
}
