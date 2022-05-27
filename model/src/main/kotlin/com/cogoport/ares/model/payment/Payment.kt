package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Introspected
data class Payment(
    var id: Long?,
    var entityType: Int,
    var fileId: Long? = null,
    var orgSerialId: Long?,
    var sageOrganizationId: String?,
    var customerId: UUID?,
    var customerName: String?,
    var accCode: Int,
    var accMode: AccMode,
    var signFlag: Int,
    var currencyType: String,
    var amount: BigDecimal,
    var ledCurrency: String,
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
