package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.CollectionTrend
import org.mapstruct.Mapper

@Mapper
interface CollectionTrendMapper {

    fun convertToModel(collectionTrend: CollectionTrend): com.cogoport.ares.model.payment.CollectionTrendResponse

    fun convertToEntity(collectionTrendResponse: com.cogoport.ares.model.payment.CollectionTrendResponse): CollectionTrend
}
