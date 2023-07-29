package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class UnPaidInvoiceSummary(
    @JsonProperty("shipment_serial_id")
    val sid: String?,
    @JsonProperty("invoice_id")
    val invoiceNumber: String?,
    @JsonProperty("pdf_url")
    val pdfUrl: String?,
    @JsonProperty("invoice_sub_type")
    val invoiceType: String?,
    @JsonProperty("grand_total")
    val grandTotal: String?,
    @JsonProperty("balance")
    val balance: String?,
    @JsonProperty("due_date")
    val dueDate: String?,
    @JsonProperty("relative_duration")
    val relativeDuration: String?,
)
