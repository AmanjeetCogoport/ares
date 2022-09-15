package com.cogoport.ares.model.payment.response

import java.util.UUID

data class CustomerListCountResponse(
    val bookingPartyId: UUID,
    val proformaNumbers: List<String>,
    val dueCount: Long,
    val overdueCount: Long,
    val proformaCount: Long,
    val thirtyCount: Int,
    val sixtyCount: Int,
    val ninetyCount: Int,
    val ninetyPlus: Int,
)
