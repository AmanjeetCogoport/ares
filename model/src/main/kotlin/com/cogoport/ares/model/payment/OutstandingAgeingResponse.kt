package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

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
