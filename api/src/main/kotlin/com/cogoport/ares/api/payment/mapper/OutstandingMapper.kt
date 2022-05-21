package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.Dso
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.model.payment.OutstandingResponse
import org.mapstruct.Mapper

@Mapper
interface OutstandingMapper {

    fun convertToModel(outstanding: Outstanding): OutstandingResponse

    fun convertToEntity(outstanding: OutstandingResponse): Dso
}
