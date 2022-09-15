package com.cogoport.ares.model.payment.response

import java.util.UUID

data class ConsolidatedAddresses(
    val bookingPartyId: UUID?,
    val proformaNumbers: List<String?>,
    val bookingPartyName: String?,
)
