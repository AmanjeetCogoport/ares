package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
data class Payment(
    var id: Long? = 0,
    @field:NotNull(message = "Entity Type is required")
    var entityType: Int = 0,
    var fileId: Long? = null,
    var orgSerialId: Long? = null,
    var sageOrganizationId: String? = null,
    var customerId: UUID? = null,
    var customerName: String? = "",
    @field:NotNull(message = "Account Code is required")
    var accCode: Int = 0,
    @field:NotNull(message = "Account Mode is required")
    var accMode: AccMode? = AccMode.AR,
    var signFlag: Short = 1,
    @field:NotNull(message = "Currency Type is required")
    var currencyType: String = "",
    @field:NotNull(message = "Amount is required")
    var amount: BigDecimal = 0.toBigDecimal(),
    @field:NotNull(message = "Ledger Currency is required")
    var ledCurrency: String = "INR",
    @field:NotNull(message = "Ledger Amount is required")
    var ledAmount: BigDecimal = 0.toBigDecimal(),
    var payMode: PayMode? = null,
    var remarks: String? = null,
    var utr: String? = "",
    var refPaymentId: Long? = 0,
    var transactionDate: Timestamp? = Timestamp(System.currentTimeMillis()),
    var isPosted: Boolean = false,
    var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    @JsonProperty("bankAccountNumber")
    var bankAccountNumber: String? = "",
    var zone: String? = "",
    var serviceType: String? = "",
    var paymentCode: PaymentCode?
)
