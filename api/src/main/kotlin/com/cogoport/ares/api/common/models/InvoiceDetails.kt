package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class InvoiceDetails(
        @JsonProperty("invoice_pdf_url")
        var invoiceUrl: String?
)

