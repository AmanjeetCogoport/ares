package com.cogoport.ares.api.payment.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

@MappedEntity
data class Invoice(
    @JsonProperty("id") var id: Long?,
    @JsonProperty("jobId") var  jobId: Long,
    var status: String?,
    var placeOfSupply: String,
    var currency: String,
    var exchangeRate: BigDecimal,
    var creditDays: Short = 0,
    var proformaNumber: String,
    var proformaPdfUrl: String?,
    var proformaDate: Date,
    var terms: String?,
    var invoiceType: String?,
    var discount_amount: BigDecimal = 0.toBigDecimal(),
    var subTotal: BigDecimal,
    var taxTotal: BigDecimal,
    var grandTotal: BigDecimal,
    var ledgerTotal: BigDecimal,
    var ledgerCurrency: String = "INR",
    var invoiceNumber: String?,
    var invoicePdfUrl: String?,
    var invoiceDate: Date?,
    var createdBy: UUID,
    var updatedBy: UUID,
    var createdAt: Date,
    var updatedAt: Date,
    var organizationId: String?
)