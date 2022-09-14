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
    val thirtyAmount: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("sixtyAmount")
    val sixtyAmount: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("ninetyAmount")
    val ninetyAmount: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("ninetyPlusAmount")
    val ninetyPlusAmount: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("thirtyCount")
    val thirtyCount: Int? = 0,
    @JsonProperty("sixtyCount")
    val sixtyCount: Int? = 0,
    @JsonProperty("ninetyCount")
    val ninetyCount: Int? = 0,
    @JsonProperty("ninetyPlusCount")
    val ninetyPlusCount: Int? = 0
)
