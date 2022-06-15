package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class DeletePaymentRequest(
    @JsonProperty("paymentId")
    var paymentId: Long
)
