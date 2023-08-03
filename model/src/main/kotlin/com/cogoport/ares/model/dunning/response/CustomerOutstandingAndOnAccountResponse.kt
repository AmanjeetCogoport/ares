package com.cogoport.ares.model.dunning.response

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity
data class CustomerOutstandingAndOnAccountResponse(
    var tradePartyDetailId: UUID,
    var entityCode: Int,
    var ledCurrency: String,
    var id: Long,
    var tradePartyDetailName: String,
    var taggedOrganizationId: UUID,
    var outstandingAmount: BigDecimal,
    var onAccountAmount: BigDecimal
)
