package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
@JsonInclude
data class BankDetails(
    @JsonProperty("id")
    var id: UUID? = null,
    @JsonProperty("partner_id")
    var partnerId: String? = null,
    @JsonProperty("account_number")
    var accountNumber: String? = null,
    @JsonProperty("beneficiary_name")
    var beneficiaryName: String? = null,
    @JsonProperty("account_type")
    var accountType: String? = null,
    @JsonProperty("bank_name")
    var bankName: String? = null,
    @JsonProperty("ifsc_code")
    var ifscCode: String? = null,
    @JsonProperty("swift_code")
    var swiftCode: String? = null,
    @JsonProperty("bank_code")
    var bankCode: String? = null,
    @JsonProperty("branch_code")
    var branchCode: String? = null,
    @JsonProperty("status")
    var status: String? = null,
    @JsonProperty("can_receive")
    var canReceive: Boolean? = null,
    @JsonProperty("can_pay")
    var canPay: Boolean? = null,
    @JsonProperty("created_at")
    var createdAt: Any? = null,
    @JsonProperty("updated_at")
    var updatedAt: Any? = null,
    @JsonProperty("currency")
    var currency: String? = null
)
