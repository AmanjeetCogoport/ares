package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class JournalVoucherRecord(
    @JsonProperty("entity_code")
    val entityCode: Int? = null,

    @JsonProperty("org_serial_id")
    val organizationSerialId: UUID? = null,

    @JsonProperty("sage_organization_id")
    val sageOrganizationId: String? = null,

    @JsonProperty("organization_id")
    val organizationId: UUID? = null,

    @JsonProperty("organization_name")
    val organizationName: String? = null,

    @JsonProperty("acc_code")
    val accCode: Int? = null,

    @JsonProperty("acc_mode")
    val accMode: String? = null,

    @JsonProperty("pay_mode")
    val paymentMode: String? = null,

    @JsonProperty("narration")
    val narration: String? = null,

    @JsonProperty("transaction_date")
    val transactionDate: Timestamp? = null,

    @JsonProperty("due_date")
    val dueDate: Timestamp? = null,

    @JsonProperty("created_at")
    val createdAt: Timestamp? = null,

    @JsonProperty("updated_at")
    val updatedAt: Timestamp? = null,

    @JsonProperty("payment_code")
    val paymentCode: String? = null,

    @JsonProperty("payment_num")
    val paymentNum: String? = null,

    @JsonProperty("account_util_amt_curr")
    val accountUtilAmtCurr: BigDecimal,

    @JsonProperty("account_util_amt_led")
    val accountUtilAmtLed: BigDecimal,

    @JsonProperty("account_util_pay_curr")
    val accountUtilPayCurr: BigDecimal,

    @JsonProperty("account_util_pay_led")
    val accountUtilPayLed: BigDecimal,

    @JsonProperty("sign_flag")
    val signFlag: Short? = null,

    @JsonProperty("currency")
    val currency: String? = null,

    @JsonProperty("led_currency")
    val ledgerCurrency: String? = null,

    @JsonProperty("account_type")
    var accountType: String? = null,

    @JsonProperty("exchange_rate")
    val exchangeRate: BigDecimal? = null,

    @JsonProperty("sage_unique_id")
    val sageUniqueId: String? = null
)
