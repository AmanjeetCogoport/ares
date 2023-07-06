package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import javax.persistence.Transient

@MappedEntity
data class KamWiseOutstanding(
    @JsonProperty("kam_owners")
    var kamOwners: String? = null,
    @JsonProperty("open_invoice_amount")
    var openInvoiceAmount: BigDecimal = BigDecimal.ZERO,
    @JsonProperty("total_outstanding_amount")
    var totalOutstandingAmount: BigDecimal = BigDecimal.ZERO
) {
    @field:Transient
    var entityCode: Int? = null

    @field:Transient
    var dashboardCurrency: String? = null
}
