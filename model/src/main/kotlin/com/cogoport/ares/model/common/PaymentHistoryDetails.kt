package com.cogoport.ares.model.common

import io.micronaut.data.annotation.MappedEntity

@MappedEntity()
data class PaymentHistoryDetails(
    var delayedPaymentsCredit: Int? = 0,
    var delayedPaymentsCash: Int? = 0,
    var totalPayments: Int? = 0
)
