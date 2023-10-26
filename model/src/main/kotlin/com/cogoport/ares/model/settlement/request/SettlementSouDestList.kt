package com.cogoport.ares.model.settlement.request

import io.micronaut.data.annotation.MappedEntity

@MappedEntity
data class SettlementSouDestList(
    var sourceId: Long,
    var sourceType: String,
    var destinationId: Long,
    var destinationType: String
)
