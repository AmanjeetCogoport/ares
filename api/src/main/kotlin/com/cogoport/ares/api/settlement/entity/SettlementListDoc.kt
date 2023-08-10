package com.cogoport.ares.api.settlement.entity

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
    var settlementDate: Date,
    val amount: BigDecimal,
    val ledAmount: BigDecimal,
    val currency: String,
    val ledCurrency: String,
    val sourceAccType: AccountType,
    val destinationAccType: AccountType,
    val sourceId: Long,
    val destinationId: Long,
    val destinationOpenInvoiceAmount: BigDecimal,
    val destinationInvoiceAmount: BigDecimal
) {
    @field:javax.persistence.Transient
    var sourceIrnNumber: String? = null
    @field:javax.persistence.Transient
    var destinationIrnNumber: String? = null
}
