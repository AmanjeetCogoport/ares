package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class OutstandingAgeingResponse(
    @JsonProperty("organizationId")
    val organizationId: String?,
    @JsonProperty("notDueAmount")
    val notDueAmount: BigDecimal?,
    @JsonProperty("thirtyAmount")
    val thirtyAmount: BigDecimal?,
    @JsonProperty("sixtyAmount")
    val sixtyAmount: BigDecimal?,
    @JsonProperty("ninetyAmount")
    val ninetyAmount: BigDecimal?,
    @JsonProperty("oneeightyAmount")
    val oneeightyAmount: BigDecimal?,
    @JsonProperty("threesixfiveAmount")
    val threesixfiveAmount: BigDecimal?,
    @JsonProperty("threesixfiveplusAmount")
    val threesixfiveplusAmount: BigDecimal?,
    @JsonProperty("notDueCount")
    val notDueCount: Int,
    @JsonProperty("thirtyCount")
    val thirtyCount: Int,
    @JsonProperty("sixtyCount")
    val sixtyCount: Int,
    @JsonProperty("ninetyCount")
    val ninetyCount: Int,
    @JsonProperty("oneeightyCount")
    val oneeightyCount: Int,
    @JsonProperty("threesixfiveCount")
    val threesixfiveCount: Int,
    @JsonProperty("threesixfiveplusCount")
    val threesixfiveplusCount: Int
)
