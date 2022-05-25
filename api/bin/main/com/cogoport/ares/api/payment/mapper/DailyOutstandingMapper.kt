package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.CollectionTrend
import com.cogoport.ares.api.payment.entity.DailyOutstanding
import com.cogoport.ares.model.payment.DailyOutstandingResponse
import org.mapstruct.Mapper

@Mapper
interface DailyOutstandingMapper {
    fun convertToModel(dso: DailyOutstanding): DailyOutstandingResponse

    fun convertToEntity(dso: DailyOutstandingResponse): DailyOutstanding
}