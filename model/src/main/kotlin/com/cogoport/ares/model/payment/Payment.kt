package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
data class Payment(
    var id: Long?,
    var entityType: Int,
    var entityId: UUID,
    var fileId: Long? = null,
    var orgSerialId: Long,
    var customerId: UUID,
    var customerName: String,
    var sageOrganizationId: String?,
    var accCode: Int,
    var accMode: AccMode,
    var signFlag: Int,
    var currencyType: String,
    var amount: BigDecimal,
    var ledCurrency: String,
    var ledAmount: BigDecimal,
    var payMode: PayMode?,
    var remarks: String? = null,
    var bankId: Int?,
    var utr: String?,
    var refPaymentId: Long?,
    var transactionDate: Timestamp?,
    var isPosted: Boolean,
    var isDeleted: Boolean,
    var createdAt: Timestamp?,
    var updatedAt: Timestamp?
)
