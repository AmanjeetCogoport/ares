package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
@JsonIgnoreProperties(ignoreUnknown = true)
data class OverdueInvoicesResponse(
    @JsonProperty("thirtyAmount")
    val thirtyAmount: BigDecimal?,
    @JsonProperty("sixtyAmount")
    val sixtyAmount: BigDecimal?,
    @JsonProperty("ninetyAmount")
    val ninetyAmount: BigDecimal?,
    @JsonProperty("ninetyPlusAmount")
    val ninetyPlusAmount: BigDecimal?,
    @JsonProperty("thirtyCount")
    val thirtyCount: Int?,
    @JsonProperty("sixtyCount")
    val sixtyCount: Int?,
    @JsonProperty("ninetyCount")
    val ninetyCount: Int?,
    @JsonProperty("ninetyPlusCount")
    val ninetyPlusCount: Int?,
)
