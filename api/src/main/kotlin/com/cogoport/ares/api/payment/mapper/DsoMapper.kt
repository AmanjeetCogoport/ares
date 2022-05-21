package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.Dso
import org.mapstruct.Mapper

@Mapper
interface DsoMapper {

    fun convertToModel(dso: Dso): com.cogoport.ares.model.payment.Dso

    fun convertToEntity(dso: com.cogoport.ares.model.payment.Dso): Dso
}