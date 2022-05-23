package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.Dso
import org.mapstruct.Mapper

@Mapper
interface DsoMapper {

    fun convertToModel(dso: Dso): com.cogoport.ares.model.payment.DsoResponse

    fun convertToEntity(dsoResponse: com.cogoport.ares.model.payment.DsoResponse): Dso
}
