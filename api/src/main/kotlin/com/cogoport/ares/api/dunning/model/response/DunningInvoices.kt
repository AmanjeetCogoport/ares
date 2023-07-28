package com.cogoport.ares.api.dunning.model.response

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date

@MappedEntity
data class DunningInvoices(
    var documentNo: Long,
    var documentValue: String,
    var ledCurrency: String,
    var amountLoc: BigDecimal,
    var payLoc: BigDecimal,
    var dueDate: Date,
    var invoiceType: String,
    var relativeDuration: String,
) {
    @javax.persistence.Transient
    var jobNumber: String? = null

    @javax.persistence.Transient
    var pdfUrl: String? = null
}
