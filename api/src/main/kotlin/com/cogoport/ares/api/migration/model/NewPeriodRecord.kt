package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp

@Introspected
data class NewPeriodRecord(
    @JsonProperty("acc_type")
    val accType: String?,
    @JsonProperty("document_value")
    val documentValue: String?,
    @JsonProperty("cogo_entity")
    val cogoEntity: String?,
    @JsonProperty("currency")
    val currency: String?,
    @JsonProperty("acc_mode")
    val accMode: String?,
    @JsonProperty("sage_organization_id")
    val sageOrganizationId: String?,
    @JsonProperty("transaction_date")
    val transactionDate: String?,
    @JsonProperty("amount_curr")
    val amountCurr: String?,
    @JsonProperty("amount_loc")
    val amountLoc: String?,
    @JsonProperty("pay_curr")
    val payCurr: String?,
    @JsonProperty("pay_loc")
    val payLoc: String?,
    @JsonProperty("created_at")
    val createdAt: Timestamp?,
    @JsonProperty("updated_at")
    val updatedAt: Timestamp?,
    @JsonProperty("sign_flag")
    val signFlag: String?
)
