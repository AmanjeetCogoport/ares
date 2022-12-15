package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class InvoiceListRequestForTradeParty(
    val docValues: List<String>,
    val pageIndex: Int,
    val pageSize: Int,
    val sortType: String?,
    val sortBy: String?
)
