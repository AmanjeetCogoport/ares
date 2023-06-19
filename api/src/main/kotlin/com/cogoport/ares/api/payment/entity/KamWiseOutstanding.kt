package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class KamWiseOutstanding(
    @JsonProperty("kam_owners")
    var kamOwners: String? = null,
    @JsonProperty("open_invoice_amount")
    var openInvoiceAmount: BigDecimal = BigDecimal.ZERO,
    @JsonProperty("total_outstanding_amount")
    var totalOutstandingAmount: BigDecimal = BigDecimal.ZERO,
    @JsonProperty("entity_code")
    var entityCode: Int? = null
)
