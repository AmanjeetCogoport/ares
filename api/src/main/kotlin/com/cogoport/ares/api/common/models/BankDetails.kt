package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Timestamp

class BankDetails(
    @JsonProperty("id")
    var id: String? = null,

    @JsonProperty("partner_id")
    var partnerId: String? = null,

    @JsonProperty("account_number")
    var accountNumber: String? = null,

    @JsonProperty("beneficiary_name")
    var beneficiaryName: String? = null,

    @JsonProperty("account_type")
    var accountType: String? = null,

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
    var createdAt: Timestamp? = null,

    @JsonProperty("updated_at")
    var updatedAt: Timestamp? = null,

    @JsonProperty("currency")
    var currency: String? = null
)
