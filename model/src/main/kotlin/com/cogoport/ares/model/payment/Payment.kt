package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Introspected
data class Payment(
    var id: Long?,
    @field:NotNull(message = "Entity Type is required")
    var entityType: Int,
    var fileId: Long? = null,
    var orgSerialId: Long?,
    var sageOrganizationId: String?,
    var customerId: UUID?,
    var customerName: String?,
    @field:NotNull(message = "Account Code is required")
    var accCode: Int,
    @field:NotNull(message = "Account Mode is required")
    var accMode: AccMode,
    var signFlag: Int,
    @field:NotNull(message = "Currency Type is required")
    var currencyType: String,
    @field:NotNull(message = "Amount is required")
    var amount: BigDecimal,
    @field:NotNull(message = "Ledger Currency is required")
    var ledCurrency: String,
    @field:NotNull(message = "Ledger Amount is required")
    var ledAmount: BigDecimal,
    var payMode: PayMode?,
    var remarks: String? = null,
    var bankId: Int,
    var utr: String?,
    var refPaymentId: Long?,
    var transactionDate: LocalDate?,
    var isPosted: Boolean,
    var isDeleted: Boolean,
    var createdAt: LocalDateTime?,
    var modifiedAt: LocalDateTime?
)
