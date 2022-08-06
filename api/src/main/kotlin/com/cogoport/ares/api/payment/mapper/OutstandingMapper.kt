package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.Dso
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.model.payment.response.OutstandingResponse
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface OutstandingMapper {

    fun convertToModel(outstanding: Outstanding): OutstandingResponse

    fun convertToEntity(outstanding: OutstandingResponse): Dso
}
