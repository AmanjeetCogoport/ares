package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class OutstandingAgeingResponse(
    @JsonProperty("organization_id")
    val organization_id: String,
    @JsonProperty("not_due_amount")
    val not_due_amount: BigDecimal?,
    @JsonProperty("thirty_amount")
    val thirty_amount: BigDecimal?,
    @JsonProperty("sixty_amount")
    val sixty_amount: BigDecimal?,
    @JsonProperty("ninety_amount")
    val ninety_amount: BigDecimal?,
    @JsonProperty("oneeighty_amount")
    val oneeighty_amount: BigDecimal?,
    @JsonProperty("threesixfive_amount")
    val threesixfive_amount: BigDecimal?,
    @JsonProperty("threesixfiveplus_amount")
    val threesixfiveplus_amount: BigDecimal?,
    @JsonProperty("not_due_count")
    val not_due_count: Int,
    @JsonProperty("thirty_count")
    val thirty_count: Int,
    @JsonProperty("sixty_count")
    val sixty_count: Int,
    @JsonProperty("ninety_count")
    val ninety_count: Int,
    @JsonProperty("oneeighty_count")
    val oneeighty_count: Int,
    @JsonProperty("threesixfive_count")
    val threesixfive_count: Int,
    @JsonProperty("threesixfiveplus_count")
    val threesixfiveplus_count: Int
)
