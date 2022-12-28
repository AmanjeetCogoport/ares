package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class CustomerStatsRequest(
    val docValues: List<String>?,
    val bookingPartyId: String?,
    val pageIndex: Int,
    val pageSize: Int,
    val sortType: String?,
    val sortBy: String?
)
