package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.DailyOutstanding
import com.cogoport.ares.model.payment.response.DailyOutstandingResponseData
import org.mapstruct.Mapper

@Mapper
interface DailyOutstandingMapper {
    fun convertToModel(dso: DailyOutstanding): DailyOutstandingResponseData

    fun convertToEntity(dso: DailyOutstandingResponseData): DailyOutstanding
}
