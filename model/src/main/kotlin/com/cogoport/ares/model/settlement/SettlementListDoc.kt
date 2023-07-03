package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccountType
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date

@Introspected
@MappedEntity
data class SettlementListDoc(
    val id: String,
    val sourceDocumentValue: String,
    val destinationDocumentValue: String,
    val settlementDate: Date,
    val entityCode: Int,
    val amount: BigDecimal,
    val ledAmount: BigDecimal,
    val currency: String,
    val ledCurrency: String,
    var sourceIrnNumber: String?,
    var destinationIrnNumber: String?,
    val sourceAccType: AccountType,
    val destinationAccType: AccountType,
    val sourceId: Long,
    val destinationId: Long
)
