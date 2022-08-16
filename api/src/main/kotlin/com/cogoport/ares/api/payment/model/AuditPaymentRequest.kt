package com.cogoport.ares.api.payment.model

import com.cogoport.ares.api.payment.entity.Payment

data class AuditPaymentRequest(
    var payment: Payment,
    var actionName: String,
    var performedById: String?,
    var performedByType: String?
)
