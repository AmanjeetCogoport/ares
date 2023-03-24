package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class QsoResponse(
    @JsonProperty("quarter")
    var quarter: String,
    @JsonProperty("qsoForQuarter")
    var qsoForQuarter: BigDecimal,
    @JsonProperty("currency")
    var currency: String?
)
