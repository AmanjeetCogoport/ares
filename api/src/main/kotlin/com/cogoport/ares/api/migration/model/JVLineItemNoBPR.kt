package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Introspected
data class JVLineItemNoBPR(
    @JsonProperty("entityCode")
    val entityCode: String?,
    @JsonProperty("jvNum")
    val jvNum: String,
    @JsonProperty("type")
    val type: String,
    @JsonProperty("validityDate")
    val validityDate: Date,
    @JsonProperty("amount")
    val amount: BigDecimal,
    @JsonProperty("ledger_amount")
    val ledgerAmount: BigDecimal,
    @JsonProperty("currency")
    val currency: String,
    @JsonProperty("ledgerCurrency")
    val ledgerCurrency: String,
    @JsonProperty("status")
    val status: String,
    @JsonProperty("exchange_rate")
    val exchangeRate: BigDecimal,
    @JsonProperty("created_at")
    val createdAt: Timestamp,
    @JsonProperty("updated_at")
    val updatedAt: Timestamp,
    @JsonProperty("description")
    val description: String,
    @JsonProperty("sage_unique_id")
    val sageUniqueId: String,
    @JsonProperty("sign_flag")
    val signFlag: BigDecimal,
    @JsonProperty("gl_code")
    val glcode: String
)
