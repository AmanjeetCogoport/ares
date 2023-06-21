package com.cogoport.ares.api.payment.model.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date

@Introspected
@MappedEntity
data class DocumentResponse(
    val taggedOrganizationId: String,
    val currency: String,
    val ledCurrency: String,
    val signFlag: Short,
    val amountCurr: BigDecimal,
    val payCurr: BigDecimal,
    val amountLoc: BigDecimal,
    val payLoc: BigDecimal,
    val transactionDate: Date,
    val dueDate: Date,
    val entityCode: Int
)
