package com.cogoport.ares.model.payment.response

import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity
data class CustomerListCountResponse(
    val bookingPartyId: UUID?,
    val docValues: List<String?>,
    val dueCount: Long?,
    val overdueCount: Long?,
    val proformaCount: Long?,
    val thirtyCount: Int?,
    val sixtyCount: Int?,
    val ninetyCount: Int?,
    val ninetyPlus: Int?,
)
