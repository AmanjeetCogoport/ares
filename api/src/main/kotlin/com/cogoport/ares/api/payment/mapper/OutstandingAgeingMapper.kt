package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.OutstandingAgeing
import com.cogoport.ares.model.payment.OutstandingAgeingResponse
import org.mapstruct.Mapper

@Mapper
interface   OutstandingAgeingMapper {
    fun convertToModel(outstandingAgeing: OutstandingAgeing): OutstandingAgeingResponse

    fun convertToEntity(outstandingAgeing: OutstandingAgeingResponse): OutstandingAgeing
}