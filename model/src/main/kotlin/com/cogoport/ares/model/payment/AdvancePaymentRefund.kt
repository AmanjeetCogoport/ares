package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID
import javax.validation.constraints.Min

@JsonInclude
@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdvancePaymentRefund(
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

  // Trader partner details id
  @JsonProperty("customerId")
  var organizationId: UUID?,

  // Organization id of customer/service provider
  @JsonProperty("taggedOrganizationId")
  var taggedOrganizationId: UUID?,

  @JsonProperty("tradePartyMappingId")
  var tradePartyMappingId: UUID?,

  @JsonProperty("customerName")
  var organizationName: String? = "",

  @JsonProperty("accCode")
  var accCode: Int? = 0,

  @JsonProperty("accMode")
  var accMode: AccMode? = AccMode.AR,

  @JsonProperty("signFlag")
  var signFlag: Short? = 1,

  @JsonProperty("currency") @field:NotNull(message = "Currency  is required")
  var currency: String? = "",

  @field:NotNull(message = "Currency amount is required")
  @JsonProperty("amount")
  var amount: BigDecimal? = 0.toBigDecimal(),

  @JsonProperty("ledCurrency") @field:NotNull(message = "Ledger currency is required")
  var ledCurrency: String? = "INR",

  @JsonProperty("ledAmount") @field:NotNull(message = "Ledger amount is required")
  var ledAmount: BigDecimal? = 0.toBigDecimal(),

  @JsonProperty("paymentMode")
  @field:NotNull(message = "Payment mode is required")
  var payMode: PayMode? = null,

  @JsonProperty("remarks")
  var remarks: String? = null,

  @JsonProperty("utr")
  var utr: String? = "",

  @JsonProperty("refPaymentId")
  var refPaymentId: String? = null,

  @JsonProperty("refAccountNo")
  var refAccountNo: String?,

  @JsonProperty("transactionDate")
  var transactionDate: Date? = Date(),

  @JsonProperty("createdAt")
  var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),

  @JsonProperty("createdBy")
  var createdBy: String? = "",

  @JsonProperty("updatedBy")
  var updatedBy: String? = "",

  @JsonProperty("updatedAt")
  var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis()),

  @JsonProperty("bankAccountNumber")
  var bankAccountNumber: String? = "",

  @JsonProperty("zone")
  var zone: String? = "",

  @JsonProperty("serviceType")
  var serviceType: ServiceType?,

  @JsonProperty("paymentCode")
  var paymentCode: PaymentCode? = PaymentCode.REC,

  @JsonProperty("paymentDate")
  var paymentDate: String? = "",

  @JsonProperty("uploadedBy")
  var uploadedBy: String? = "",

  @JsonProperty("bankName")
  var bankName: String? = "",

  @JsonProperty("exchangeRate")
  @Min(value = 1, message = "Minimum value for exchange rate is 1")
  var exchangeRate: BigDecimal? = BigDecimal.ONE,

  @JsonProperty("receiptNumber")
  var paymentNum: Long?,

  @JsonProperty("receiptParam")
  var paymentNumValue: String?,

  @JsonProperty("bankId")
  var bankId: UUID?,

  @JsonProperty("performedByUserType")
  val performedByUserType: String? = null,

  @JsonProperty("paymentDocumentStatus")
  var paymentDocumentStatus: PaymentDocumentStatus? = PaymentDocumentStatus.CREATED,

  @JsonProperty("tradePartyDocument")
  val tradePartyDocument: String? = null,

  @JsonProperty("docType")
  val docType: DocType? = DocType.PAYMENT,

  @JsonProperty("sageRefNumber")
  var sageRefNumber: String? = null,

  @JsonProperty("preMigratedDeleted")
  var preMigratedDeleted: Boolean? = false,

  @JsonProperty("advanceDocumentIf")
  var advanceDocumentId: String? = null,

  @JsonProperty("paymentDocUrl")
  var paymentDocUrl: String? = null
)
