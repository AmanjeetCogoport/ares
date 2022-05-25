package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class OrgOutstandingResponse(
    @JsonProperty("organizationName")
    val organizationName: String?,
    @JsonProperty("openInvoiceCount")
    val openInvoiceCount: Int?,
    @JsonProperty("invoiceAmountInr")
    val invoiceAmountInr: BigDecimal?,
    @JsonProperty("invoiceAmountUsd")
    val invoiceAmountUsd: BigDecimal?,
    @JsonProperty("onAccountPaymentCount")
    val onAccountPaymentCount: Int?,
    @JsonProperty("onAccountPaymentInr")
    val onAccountPaymentInr: BigDecimal?,
    @JsonProperty("onAccountPaymentUsd")
    val onAccountPaymentUsd: BigDecimal?,
    @JsonProperty("outstandingInr")
    val outstandingInr: BigDecimal?,
    @JsonProperty("outstandingUsd")
    val outstandingUsd: BigDecimal?
)
