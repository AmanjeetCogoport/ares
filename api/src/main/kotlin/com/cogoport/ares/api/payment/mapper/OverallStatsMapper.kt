package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.OverallStats
import com.cogoport.ares.model.payment.response.OverallStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsResponseData
import org.mapstruct.Mapper

@Mapper
interface OverallStatsMapper {

    fun convertToModel(overallStats: OverallStats): OverallStatsResponseData

    fun convertToEntity(overallStats: OverallStatsResponseData): OverallStats
}
