package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@MappedEntity
@Introspected
data class PaymentResponse(
    @JsonProperty("id")
    var id: Long? = 0,

    @JsonProperty("entityType")
    var entityCode: Int? = 0,

    @JsonProperty("orgSerialId")
    var orgSerialId: Long? = null,

    @JsonProperty("customerName")
    var organizationName: String? = "",

    @JsonProperty("bankAccountNumber")
    var bankAccountNumber: String? = "",

    @JsonProperty("bankName")
    var bankName: String? = "",

    @JsonProperty("accCode")
    var accCode: Int? = 0,

    @JsonProperty("sageOrganizationId")
    var sageOrganizationId: String? = null,

    @JsonProperty("customerId")
    var organizationId: UUID? = null,

    @JsonProperty("currency")
    var currency: String? = "",

    @JsonProperty("amount")
    var amount: BigDecimal? = 0.toBigDecimal(),

    @JsonProperty("ledCurrency")
    var ledCurrency: String? = "INR",

    @JsonProperty("ledAmount")
    var ledAmount: BigDecimal? = 0.toBigDecimal(),

    @JsonProperty("updatedBy")
    var updatedBy: UUID?,

    @JsonProperty("utr")
    var utr: String? = "",

    @JsonProperty("accMode")
    var accMode: AccMode? = AccMode.AR,

    @JsonProperty("paymentCode")
    var paymentCode: PaymentCode? = PaymentCode.REC,

    @JsonProperty("paymentDocumentStatus")
    var paymentDocumentStatus: PaymentDocumentStatus? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    @JsonProperty("transactionDate")
    var transactionDate: Date?,

    @JsonProperty("uploadedBy")
    var uploadedBy: String? = "",

    @JsonProperty("exchangeRate")
    var exchangeRate: BigDecimal? = BigDecimal.ZERO,

    @JsonProperty("paymentNum")
    var paymentNum: Long? = 0L,

    @JsonProperty("paymentNumValue")
    var paymentNumValue: String? = "",

    @JsonProperty("createdAt")
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),

    @JsonProperty("deletedAt")
    var deletedAt: Timestamp? = Timestamp.from(Instant.now()),

    @JsonProperty("narration")
    var narration: String? = null
)
