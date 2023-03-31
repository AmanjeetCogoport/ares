package com.cogoport.ares.model.payment.response

import java.math.BigDecimal

data class PayblesInfoRes(
    var accountPayables: BigDecimal? = null,
    var openInvoicesCount: Long? = null,
    var organizationsCount: Long? = null,
    var openInvoicesAmount: BigDecimal? = null,
    var openInvoiceChange: BigDecimal? = null,
    var onAccountAmount: BigDecimal? = null,
    var onAccountChange: BigDecimal? = null,
    var creditNoteAmount: BigDecimal? = null,
    var creditNoteChange: BigDecimal? = null,
)
