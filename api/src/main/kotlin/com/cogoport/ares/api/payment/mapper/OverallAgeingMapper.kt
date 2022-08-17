package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.OverallAgeingStats
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import org.mapstruct.Mapper

@Mapper
interface OverallAgeingMapper {
    fun convertToModel(ageingBucket: OverallAgeingStats): OverallAgeingStatsResponse

    fun convertToEntity(ageingBucket: OverallAgeingStatsResponse): OverallAgeingStats
}
