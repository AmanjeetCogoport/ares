package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.util.Date

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomerInvoiceResponse(
    @JsonProperty("invoiceNumber") val invoiceNumber: String?,
    @JsonProperty("invoiceType") val invoiceType: String?,
    @JsonProperty("shipmentId") val shipmentId: Int?,
    @JsonProperty("shipmentType") val shipmentType: String?,
    @JsonProperty("docType") val docType: String?,
    @JsonProperty("invoiceAmount") val invoiceAmount: BigDecimal?,
    @JsonProperty("currency") var currency: String?,
    @JsonProperty("balanceAmount") val balanceAmount: BigDecimal?,
    @JsonProperty("invoiceDate") val invoiceDate: Date?,
    @JsonProperty("invoiceDueDate") val invoiceDueDate: Date?,
    @JsonProperty("overdueDays") val overdueDays: Int?,
    @JsonProperty("organizationName") val organizationName: String?,
    @JsonProperty("organizationId") val organizationId: String?
)
