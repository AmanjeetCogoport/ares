package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@JsonInclude
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PaymentResponse(
    @JsonProperty("id")
    var id: Long? = 0,

    @JsonProperty("entityType")
    var entityType: Int? = 0,

    @JsonProperty("fileId")
    var fileId: Long? = null,

    @JsonProperty("orgSerialId")
    var orgSerialId: Long? = null,

    @JsonProperty("sageOrganizationId")
    var sageOrganizationId: String? = null,

    @JsonProperty("customerId")
    var organizationId: UUID? = null,

    @JsonProperty("customerName")
    var organizationName: String? = "",

    @JsonProperty("accCode")
    var accCode: Int? = 0,

    @JsonProperty("accMode")
    var accMode: AccMode? = AccMode.AR,

    @JsonProperty("signFlag")
    var signFlag: Short? = 1,

    @JsonProperty("currency")
    var currency: String? = "",

    @JsonProperty("amount")
    var amount: BigDecimal? = 0.toBigDecimal(),

    @JsonProperty("ledCurrency")
    var ledCurrency: String? = "INR",

    @JsonProperty("ledAmount")
    var ledAmount: BigDecimal? = 0.toBigDecimal(),

    @JsonProperty("paymentMode")
    var payMode: PayMode? = null,

    @JsonProperty("remarks")
    var remarks: String? = null,

    @JsonProperty("utr")
    var utr: String? = "",

    @JsonProperty("refPaymentId")
    var refPaymentId: String? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    @JsonProperty("transactionDate")
    var transactionDate: Timestamp?,

    @JsonProperty("isPosted")
    var isPosted: Boolean? = false,

    @JsonProperty("isDeleted")
    var isDeleted: Boolean? = false,

    @JsonProperty("createdBy")
    var createdBy: String? = "",

    @JsonProperty("updatedBy")
    var updatedBy: String? = "",

    @JsonProperty("bankAccountNumber")
    var bankAccountNumber: String? = "",

    @JsonProperty("zone")
    var zone: String? = "",

    @JsonProperty("serviceType")
    var serviceType: String? = "",

    @JsonProperty("paymentCode")
    var paymentCode: PaymentCode? = PaymentCode.REC,

    @JsonProperty("uploadedBy")
    var uploadedBy: String? = "",

    @JsonProperty("bankName")
    var bankName: String? = "",

    @JsonProperty("paymentDate")
    var paymentDate: String? = "",

    @JsonProperty("exchangeRate")
    var exchangeRate: BigDecimal? = BigDecimal.ZERO,

    @JsonProperty("paymentNum")
    var paymentNum: Long? = 0L,

    @JsonProperty("paymentNumValue")
    var paymentNumValue: String? = "",

    @JsonProperty("bankId")
    var bankId: UUID?,

    @JsonProperty("taggedOrganizationId")
    var taggedOrganizationId: UUID? = null,

    @JsonProperty("tradePartyMappingId")
    var tradePartyMappingId: UUID? = null,

    @JsonProperty("tradePartyDocument")
    val tradePartyDocument: String? = null,

    @JsonProperty("paymentDocumentStatus")
    var paymentDocumentStatus: PaymentDocumentStatus? = null
)
