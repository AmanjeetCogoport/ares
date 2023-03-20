package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class InvoicesOutstandingAgeingResponse(
    @JsonProperty("organizationId")
    val organizationId: String?,
    @JsonProperty("organizationName")
    val organizationName: String?,
    @JsonProperty("notDueAmount")
    val notDueAmount: BigDecimal?,
    @JsonProperty("todayAmount")
    val todayAmount: BigDecimal?,
    @JsonProperty("thirtyAmount")
    val thirtyAmount: BigDecimal?,
    @JsonProperty("sixtyAmount")
    val sixtyAmount: BigDecimal?,
    @JsonProperty("ninetyAmount")
    val ninetyAmount: BigDecimal?,
    @JsonProperty("oneEightyAmount")
    val oneEightyAmount: BigDecimal?,
    @JsonProperty("threeSixtyFiveAmount")
    val threeSixtyFiveAmount: BigDecimal?,
    @JsonProperty("threeSixtyFvePlusAmount")
    val threeSixtyFivePlusAmount: BigDecimal?,
    @JsonProperty("totalOutstanding")
    val totalOutstanding: BigDecimal?,
    @JsonProperty("totalCreditAmount")
    val totalCreditAmount: BigDecimal?,
    @JsonProperty("totalDebitAmount")
    val totalDebitAmount: BigDecimal?,
    @JsonProperty("notDueCount")
    val notDueCount: Int,
    @JsonProperty("todayCount")
    val todayCount: Int,
    @JsonProperty("thirtyCount")
    val thirtyCount: Int,
    @JsonProperty("sixtyCount")
    val sixtyCount: Int,
    @JsonProperty("ninetyCount")
    val ninetyCount: Int,
    @JsonProperty("oneEightyCount")
    val oneEightyCount: Int,
    @JsonProperty("threeSixtyFiveCount")
    val threeSixtyFiveCount: Int,
    @JsonProperty("threeSixtyFivePlusCount")
    val threeSixtyFivePlusCount: Int,
    @JsonProperty("creditNoteCount")
    val creditNoteCount: Int,
    @JsonProperty("debitNoteCount")
    val debitNoteCount: Int
)
