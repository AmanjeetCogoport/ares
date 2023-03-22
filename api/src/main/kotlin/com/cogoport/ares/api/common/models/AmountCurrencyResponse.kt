package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class AmountCurrencyResponse(
    @JsonProperty("amount")
    var amount: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("currency")
    var currency: String
)
