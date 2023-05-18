package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class SupplierReceivableStats(
        var taggedOrganizationId: String,
        var currency: String,
        var receivablesInvoicesCount: Int,
        var totalReceivableAmount: BigDecimal,
        var totalReceivableLedAmount: BigDecimal,
        var unpaidReceivableAmount: BigDecimal,
        var unpaidReceivableLedAmount: BigDecimal,
        var unpaidInvoicesCount: Int,
        var partialPaidReceivableAmount: BigDecimal,
        var partialPaidReceivableLedAmount: BigDecimal,
        var partialPaidInvoicesCount: Int
)
