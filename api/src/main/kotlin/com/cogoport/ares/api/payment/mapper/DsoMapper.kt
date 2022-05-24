package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.Dso
import com.cogoport.ares.model.payment.DsoResponse
import org.mapstruct.Mapper

@Mapper
interface DsoMapper {

    fun convertToModel(dso: Dso): DsoResponse

    fun convertToEntity(dso: DsoResponse): Dso
}
