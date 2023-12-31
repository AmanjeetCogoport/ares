package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class BillOutStandingAgeingResponse(
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
    @JsonProperty("oneeightyAmount")
    val oneeightyAmount: BigDecimal?,
    @JsonProperty("threesixtyfiveAmount")
    val threesixtyfiveAmount: BigDecimal?,
    @JsonProperty("threesixtyfiveplusAmount")
    val threesixtyfiveplusAmount: BigDecimal?,
    @JsonProperty("totalOutstanding")
    val totalOutstanding: BigDecimal?,
    @JsonProperty("totalCreditAmount")
    val totalCreditAmount: BigDecimal?,
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
    @JsonProperty("oneeightyCount")
    val oneeightyCount: Int,
    @JsonProperty("threesixtyfiveCount")
    val threesixtyfiveCount: Int,
    @JsonProperty("threesixtyfiveplusCount")
    val threesixtyfiveplusCount: Int,
    @JsonProperty("creditNoteCount")
    val creditNoteCount: Int

)
