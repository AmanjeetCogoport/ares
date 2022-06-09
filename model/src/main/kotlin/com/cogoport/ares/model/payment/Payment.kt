package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@JsonInclude
@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Payment(
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
    @JsonProperty("transactionDate")
    var transactionDate: Timestamp? = Timestamp(System.currentTimeMillis()),
    @JsonProperty("isPosted")
    var isPosted: Boolean? = false,
    @JsonProperty("isDeleted")
    var isDeleted: Boolean? = false,
    @JsonProperty("createdAt")
    var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    @JsonProperty("createdBy")
    var createdBy: String? = "",
    @JsonProperty("updatedAt")
    var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    @JsonProperty("bankAccountNumber")
    var bankAccountNumber: String? = "",
    @JsonProperty("zone")
    var zone: String? = "",
    @JsonProperty("serviceType")
    var serviceType: String? = "",
    @JsonProperty("paymentCode")
    var paymentCode: PaymentCode? = PaymentCode.REC,
    @JsonProperty("paymentDate")
    var paymentDate: String? = "",
    @JsonProperty("uploadedBy")
    var uploadedBy: String? = "",
    @JsonProperty("bankName")
    var bankName: String? = "",
    @JsonProperty("organizationId")
    var organizationId: UUID? = null
)
