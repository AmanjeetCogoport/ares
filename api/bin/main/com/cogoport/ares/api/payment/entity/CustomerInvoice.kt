package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.*

@MappedEntity
data class CustomerInvoice(
    val invoiceNumber: String?,
    val invoiceType: String?,
    val shipmentId: Int?,
    val shipmentType: String?,
    val docType: String?,
    val invoiceAmount: BigDecimal?,
    var currency: String?,
    val balanceAmount: BigDecimal?,
    val invoiceDate: Date?,
    val invoiceDueDate: Date?,
    val overdueDays: Int?,
    val organizationName: String?,
    val organizationId: String?
)