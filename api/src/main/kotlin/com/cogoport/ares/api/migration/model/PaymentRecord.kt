package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class PaymentRecord(
    @JsonProperty("entity_code")
    val entityCode: Int? = null,

    @JsonProperty("org_serial_id")
    val organizationSerialId: UUID? = null,

    @JsonProperty("sage_organization_id")
    var sageOrganizationId: String? = null,

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
    var paymentNum: Long? = null,

    @JsonProperty("account_util_amt_curr")
    val accountUtilAmtCurr: BigDecimal,

    @JsonProperty("account_util_amt_led")
    val accountUtilAmtLed: BigDecimal,

    @JsonProperty("account_util_pay_curr")
    val accountUtilPayCurr: BigDecimal,

    @JsonProperty("account_util_pay_led")
    val accountUtilPayLed: BigDecimal,

    @JsonProperty("amount")
    val currencyAmount: BigDecimal,

    @JsonProperty("led_amount")
    val ledgerAmount: BigDecimal,

    @JsonProperty("bank_pay_amount")
    val bankPayAmount: BigDecimal? = null,

    @JsonProperty("sign_flag")
    val signFlag: Short? = null,

    @JsonProperty("currency")
    val currency: String? = null,

    @JsonProperty("led_currency")
    val ledgerCurrency: String? = null,

    @JsonProperty("exchange_rate")
    val exchangeRate: BigDecimal? = null,

    @JsonProperty("account_type")
    val accountType: String? = null,

    @JsonProperty("pan_number")
    val panNumber: String? = null,

    @JsonProperty("cogo_account_no")
    val cogoAccountNumber: String? = null,

    @JsonProperty("ref_account_no")
    val refAccountNumber: String? = null,

    @JsonProperty("bank_name")
    val bankName: String? = null,

    @JsonProperty("trans_ref_number")
    val transRefNumber: String? = null,

    @JsonProperty("is_posted")
    val isPosted: Boolean = true,

    @JsonProperty("is_deleted")
    val isDeleted: Boolean = false,

    @JsonProperty("bank_id")
    val bankId: UUID? = null,

    @JsonProperty("bank_short_code")
    val bankShortCode: String? = null,

    @JsonProperty("utilized_updated_at")
    val utilizedUpdatedAt: Timestamp? = null,

    @JsonProperty("payment_num_value")
    var paymentNumValue: String? = null,

    @JsonProperty("sage_ref_number")
    var sageRefNumber: String? = null
)
