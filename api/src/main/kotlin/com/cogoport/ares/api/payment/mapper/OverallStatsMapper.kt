package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.OverallStats
import com.cogoport.ares.model.payment.response.OverallStatsResponse
import org.mapstruct.Mapper

@Mapper
interface OverallStatsMapper {

    fun convertToModel(overallStats: OverallStats): OverallStatsResponse

    fun convertToEntity(overallStats: OverallStatsResponse): OverallStats
}
