package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class AccountPayablesStats(
    var openInvoiceAmount: BigDecimal?,
    var openInvoiceCount: Long?,
    var onAccountAmount: BigDecimal?,
    var creditNoteAmount: BigDecimal?
)
