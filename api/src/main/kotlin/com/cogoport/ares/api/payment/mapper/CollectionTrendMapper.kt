package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.CollectionTrend
import com.cogoport.ares.model.payment.response.CollectionTrendResponse
import org.mapstruct.Mapper

@Mapper
interface CollectionTrendMapper {

    fun convertToModel(collectionTrend: CollectionTrend): CollectionTrendResponse

    fun convertToEntity(collectionTrendResponse: CollectionTrendResponse): CollectionTrend
}
