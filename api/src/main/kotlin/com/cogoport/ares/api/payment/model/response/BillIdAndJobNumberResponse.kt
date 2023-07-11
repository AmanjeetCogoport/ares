package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class BillIdAndJobNumberResponse(
    val jobNumber: String,
    val id: Long
)
