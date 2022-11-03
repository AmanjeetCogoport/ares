package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.PaymentStatus
import com.cogoport.ares.model.payment.AccountType
import java.math.BigDecimal

data class InvoicePaymentResponse(
    var documentNo: Long,
    var accType: AccountType,
    var balanceAmount: BigDecimal,
    var balanceAmountInLedgerCurrency: BigDecimal,
    var paymentStatus: PaymentStatus
)
