package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaymentResponse(
    @JsonProperty("id")
    var id: Long? = 0,
    @JsonProperty("entityType") @field:NotNull(message = "Entity Type is required")
    var entityType: Int? = 0,
    @JsonProperty("fileId")
    var fileId: Long? = null,
    @JsonProperty("orgSerialId")
    var orgSerialId: Long? = null,
    @JsonProperty("sageOrganizationId")
    var sageOrganizationId: String? = null,
    @JsonProperty("customerId")
    var customerId: String? = null,
    @JsonProperty("customerName")
    var customerName: String? = "",
    @JsonProperty("accCode") @field:NotNull(message = "Account Code is required")
    var accCode: Int? = 0,
    @JsonProperty("accMode") @field:NotNull(message = "Account Mode is required")
    var accMode: AccMode? = AccMode.AR,
    @JsonProperty("signFlag")
    var signFlag: Short? = 1,
    @JsonProperty("currency") @field:NotNull(message = "Currency Type is required")
    var currencyType: String? = "",
    @JsonProperty("amount") @field:NotNull(message = "Amount is required")
    var amount: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("ledCurrency") @field:NotNull(message = "Ledger Currency is required")
    var ledCurrency: String? = "INR",
    @JsonProperty("ledAmount") @field:NotNull(message = "Ledger Amount is required")
    var ledAmount: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("paymentMode")
    var payMode: PayMode? = null,
    @JsonProperty("remarks")
    var remarks: String? = null,
    @JsonProperty("utr")
    var utr: String? = "",
    @JsonProperty("refPaymentId")
    var refPaymentId: Long? = 0,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    @JsonProperty("transactionDate")
    var transactionDate: Timestamp?,
    @JsonProperty("isPosted")
    var isPosted: Boolean? = false,
    @JsonProperty("isDeleted")
    var isDeleted: Boolean? = false,
    @JsonFormat(shape = JsonFormat.Shape.ANY, pattern = "yyyy-MM-dd hh:mm:ss")
    @JsonProperty("createdAt")
    var createdAt: Timestamp?,
    @JsonProperty("createdBy")
    var createdBy: String? = "",
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    @JsonProperty("updatedAt")
    var updatedAt: Timestamp?,
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
    @JsonProperty("organizationId")
    var organizationId: UUID? = null,
    @JsonProperty("paymentDate")
    var paymentDate: String? = ""
)
