package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp

@Introspected
@MappedEntity
data class SupplierLedgerResponse(
        var transactionDate: Timestamp?,
        var jobNumber: String?,
        var documentNo: String?,
        var type: String?,
        var debit: BigDecimal,
        var credit: BigDecimal,
        var debitBalance: BigDecimal,
        var creditBalance: BigDecimal,
        var balance: BigDecimal
)
