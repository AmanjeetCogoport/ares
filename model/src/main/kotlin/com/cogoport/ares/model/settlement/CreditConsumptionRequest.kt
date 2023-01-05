package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID
import java.util.Date
import kotlin.collections.ArrayList

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
data class CreditPaymentRequest(
    @JsonProperty("proforma_number")
    var proformaNumber: String?,
    @JsonProperty("organization_id")
    var organizationId: UUID?,
    @JsonProperty("invoice_number")
    var invoiceNumber: String,
    @JsonProperty("invoice_date")
    var invoiceDate: Date,
    @JsonProperty("invoice_due_date")
    var invoiceDueDate: String,
    @JsonProperty("invoice_amount")
    var invoiceAmount: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("paid_amount")
    var paidAmount: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("transaction_type")
    var transactionType: String?,
    @JsonProperty("currency")
    var currency: String? = "INR",
    @JsonProperty("transaction_documents")
    var documents: ArrayList<CreditPaymentDocuments>? = null,
    @JsonProperty("payment_date")
    var paymentDate: String?,
    @JsonProperty("transaction_ref_number")
    var transactionRefNumber: String? = null,
    @JsonProperty("is_irn_generated")
    var isIRNGenerated: Boolean?,
    @JsonProperty("trigger_source")
    var triggerSource: String?
)

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
data class CreditPaymentDocuments(
    @JsonProperty("type")
    var documentType: String? = null,
    @JsonProperty("url")
    var documentPdfUrl: String? = null
)
