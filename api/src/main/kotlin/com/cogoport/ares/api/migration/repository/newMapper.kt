package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.DataC
import com.cogoport.ares.api.migration.model.Data
import org.mapstruct.Mapper

@Mapper
interface newMapper {

    fun convertModelToEntity(req: Data): DataC
}
