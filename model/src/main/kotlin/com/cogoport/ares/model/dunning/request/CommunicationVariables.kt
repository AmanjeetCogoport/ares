package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
@JsonInclude(JsonInclude.Include.ALWAYS)
data class CommunicationVariables(
    @JsonProperty("customerName")
    var customerName: String? = null,
    @JsonProperty("ageing_bracket_I")
    var ageingBucket1: String? = null,
    @JsonProperty("ageing_bracket_II")
    var ageingBucket2: String? = null,
    @JsonProperty("ageing_bracket_III")
    var ageingBucket3: String? = null,
    @JsonProperty("ageing_bracket_IV")
    var ageingBucket4: String? = null,
    @JsonProperty("ageing_bracket_V")
    var ageingBucket5: String? = null,
    @JsonProperty("ageing_bracket_VI")
    var ageingBucket6: String? = null,
    @JsonProperty("total_outstanding")
    var totalOutstanding: String? = null,
    @JsonProperty("unpaid_invoices_summary")
    var unpaidInvoiceSummary: MutableList<UnPaidInvoiceSummary>? = mutableListOf(),
    @JsonProperty("signatory")
    var signatory: String? = null,
    @JsonProperty("bankDetails")
    var bankDetails: String? = null,
    @JsonProperty("payment_summary")
    var paymentSummary: MutableList<PaymentSummary>? = mutableListOf(),
    @JsonProperty("on_account")
    var onAccount: String? = null,
    @JsonProperty("contactDetails")
    var contactDetails: String? = null,
    @JsonProperty("severity_mail")
    var severityMail: String? = null,
    @JsonProperty("add_user_url")
    var addUserUrl: String? = null,
    @JsonProperty("invoice_url")
    var invoiceUrl: String? = null,
    @JsonProperty("payment_url")
    var paymentUrl: String? = null,
    @JsonProperty("feedback_url")
    var feedbackUrl: String? = null,
    @JsonProperty("ticket_url")
    var ticketUrl: String? = null,
    @JsonProperty("from_date")
    var fromDate: String? = null,
    @JsonProperty("to_date")
    var toDate: String? = null,
    @JsonProperty("bank_name")
    var bankName: String? = null,
    @JsonProperty("account_number")
    var accountNumber: String? = null,
    @JsonProperty("credit_controller_name")
    var creditControllerName: String? = null,
    @JsonProperty("credit_controller_email")
    var creditControllerEmail: String? = null,
    @JsonProperty("credit_controller_mobile_code")
    var creditControllerMobileCode: String? = null,
    @JsonProperty("credit_controller_mobile_number")
    var creditControllerMobileNumber: String? = null,
    @JsonProperty("beneficiary_name")
    var beneficiaryName: String? = null,
    @JsonProperty("ifsc_code")
    var ifscCode: String? = null,
    @JsonProperty("swift_code")
    var swiftCode: String? = null
)
