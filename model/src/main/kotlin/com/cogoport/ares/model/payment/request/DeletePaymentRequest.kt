package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.payment.AccMode
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
data class DeletePaymentRequest(
    @JsonProperty("paymentId")
    var paymentId: Long,
    @JsonProperty("performedById")
    var performedById: String? = null,
    @JsonProperty("performedByUserType")
    var performedByUserType: String? = null,
    @JsonProperty("accMode")
    var accMode: AccMode? = AccMode.AR,
)
