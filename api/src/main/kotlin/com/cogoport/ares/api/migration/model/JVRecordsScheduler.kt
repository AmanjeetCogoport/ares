package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp

@Introspected
data class JVRecordsScheduler(
    @JsonProperty("entity_code")
    val entityCode: String?,
    @JsonProperty("sage_organization_id")
    val sageOrganizationId: String?,
    @JsonProperty("payment_num")
    val paymentNum: String?,
    @JsonProperty("acc_code")
    val accCode: String?,
    @JsonProperty("acc_mode")
    val accMode: String?,
    @JsonProperty("payment_code")
    val paymentCode: String?,
    @JsonProperty("account_util_amt_curr")
    val accountUtilAmtCurr: String?,
    @JsonProperty("account_util_amt_led")
    val accountUtilAmtLed: String?,
    @JsonProperty("account_util_pay_curr")
    val accountUtilPayCurr: String?,
    @JsonProperty("account_util_pay_led")
    val accountUtilPayLed: String?,
    @JsonProperty("sign_flag")
    val signFlag: String?,
    @JsonProperty("account_type")
    val accountType: String?,
    @JsonProperty("sage_unique_id")
    val sageUniqueId: String?,
    @JsonProperty("updated_at")
    val updatedAt: Timestamp?
)
