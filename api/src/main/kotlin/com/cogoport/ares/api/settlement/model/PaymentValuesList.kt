package com.cogoport.ares.api.settlement.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude
data class PaymentValuesList(
    @JsonProperty("paymentValue")
    var paymentValue: String,
    @JsonProperty("amount")
    var amount: BigDecimal,
    @JsonProperty("flag")
    var flag: String?

)
