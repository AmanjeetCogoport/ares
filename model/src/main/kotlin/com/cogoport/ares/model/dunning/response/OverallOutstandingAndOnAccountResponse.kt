package com.cogoport.ares.model.dunning.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class OverallOutstandingAndOnAccountResponse(
    var tradePartyDetailId: String?,
    var entityCode: String?,
    var ledCurrency: String?,
    var tradePartyDetailName: String?,
    var id: String?,
    var taggedOrganizationId: String?,
    var openInvoiceAmount: BigDecimal?,
    var onAccountAmount: BigDecimal?,
    var outstandingAmount: BigDecimal?,
    var organizationStakeholderName: String?
)
