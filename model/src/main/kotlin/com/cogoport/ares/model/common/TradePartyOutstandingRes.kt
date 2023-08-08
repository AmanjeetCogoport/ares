package com.cogoport.ares.model.common

import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import java.math.BigDecimal
import java.util.UUID
import javax.persistence.Transient

@MappedEntity
data class TradePartyOutstandingRes(
    @MappedProperty("organization_id")
    val tradePartyDetailId: UUID,
    val openInvoicesLedAmount: BigDecimal,
    val overdueOpenInvoicesLedAmount: BigDecimal,
    val openInvoicesCount: Int,
    val outstandingLedAmount: BigDecimal,
    val entityCode: Int,
    val ledCurrency: String,
) {
    @field: Transient
    var registrationNumber: String? = null
}
