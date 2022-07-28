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
    val sageOrganizationId: String? = null,
    @JsonProperty("organization_id")
    val organizationId: UUID? = null,
    @JsonProperty("organization_name")
    val organizationName: String? = null,
    @JsonProperty("acc_code")
    val accCode: Int? = null,
    @JsonProperty("acc_mode")
    val accMode: String? = null,
    @JsonProperty("sign_flag")
    val signFlag: Short? = null,
    @JsonProperty("currency")
    val currency: String? = null,
    @JsonProperty("amount")
    val amount: BigDecimal? = null,
    @JsonProperty("led_currency")
    val ledCurrency: String? = null,
    @JsonProperty("led_amount")
    val ledAmount: BigDecimal? = null,
    @JsonProperty("pay_mode")
    val paymentMode: String? = null,
    @JsonProperty("narration")
    val narration: String? = null,
    @JsonProperty("cogo_account_no")
    val CogoAccountNumber: String? = null,
    @JsonProperty("ref_account_no")
    val refAccountNumber: String? = null,
    @JsonProperty("bank_name")
    val bankName: String? = null,
    @JsonProperty("trans_ref_number")
    val transRefNumber: String? = null,
    @JsonProperty("transaction_date")
    val transactionDate: Timestamp? = null,
    @JsonProperty("is_posted")
    val isPosted: Boolean = true,
    @JsonProperty("is_deleted")
    val isDeleted: Boolean = false,
    @JsonProperty("created_at")
    val createdAt: Timestamp? = null,
    @JsonProperty("updated_at")
    val updatedAt: Timestamp? = null,
    @JsonProperty("payment_code")
    val paymentCode: String? = null,
    @JsonProperty("payment_num")
    val paymentNum: String? = null,
    @JsonProperty("payment_num_value")
    val paymentNumValue: String? = null,
    @JsonProperty("exchange_rate")
    val exchangeRate: BigDecimal? = null,
    @JsonProperty("bank_id")
    val bankId: UUID? = null
)
