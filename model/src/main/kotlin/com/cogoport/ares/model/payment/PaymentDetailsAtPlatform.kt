package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date

@Introspected
@MappedEntity
data class PaymentDetailsAtPlatform(
    val sageRefNumber: String?,
    var paymentNumValue: String?,
    var sageOrganizationId: String?,
    var accCode: Long?,
    var currency: String?,
    var entityCode: Long?,
    var amount: BigDecimal? = BigDecimal.ZERO,
    var paymentDocumentStatus: String?,
    var organizationName: String?,
    var panNumber: String?,
    var accMode: String?,
    var utr: String?,
    var paymentCode: String?,
    var transactionDate: Date?,
)
