package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp

@Introspected
data class SettlementRecord(
    @JsonProperty("entity_code")
    var entityCode: String?,
    @JsonProperty("sage_organization_id")
    var sageOrganizationId: String?,
    @JsonProperty("payment_num")
    var paymentNumValue: String?,
    @JsonProperty("destination_type")
    var destinationType: String?,
    @JsonProperty("source_type")
    var sourceType: String?,
    @JsonProperty("invoice_id")
    var invoiceId: String?,
    @JsonProperty("currency")
    var currency: String?,
    @JsonProperty("ledger_currency")
    var ledger_currency: String?,
    @JsonProperty("acc_mode")
    var accMode: String?,
    @JsonProperty("acc_code")
    var accCode: String?,
    @JsonProperty("currency_amount")
    var currencyAmount: BigDecimal?,
    @JsonProperty("ledger_amount")
    var ledgerAmount: BigDecimal?,
    @JsonProperty("created_at")
    var createdAt: Timestamp?,
    @JsonProperty("updated_at")
    var updatedAt: Timestamp?
)
