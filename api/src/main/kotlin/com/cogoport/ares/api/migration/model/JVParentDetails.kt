package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class JVParentDetails(
    @JsonProperty("jv_num")
    val jvNum: String,
    @JsonProperty("jv_type")
    val jvType: String,
    @JsonProperty("jv_status")
    val jvStatus: String,
    @JsonProperty("created_at")
    val createdAt: Timestamp,
    @JsonProperty("updated_at")
    val updatedAt: Timestamp,
    @JsonProperty("validity_date")
    val validityDate: Timestamp,
    @JsonProperty("currency")
    val currency: String,
    @JsonProperty("ledger_currency")
    val ledgerCurrency: String,
    @JsonProperty("exchange_rate")
    val exchangeRate: BigDecimal,
    @JsonProperty("amount")
    val amount: BigDecimal,
    @JsonProperty("description")
    val description: String
)
