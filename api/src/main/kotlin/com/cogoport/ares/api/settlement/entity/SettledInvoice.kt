package com.cogoport.ares.api.settlement.entity

import com.cogoport.ares.model.payment.InvoiceStatus
import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.Date

@MappedEntity(value = "settlements")
data class SettledInvoice(
    @field:Id @GeneratedValue
    val id: Long?,
    val destinationId: Long,
    val destinationType: SettlementType,
    val currency: String?,
    val currentBalance: BigDecimal?,
    val amount: BigDecimal?,
    val ledCurrency: String,
    val ledAmount: BigDecimal,
    var signFlag: Short,
    val settlementDate: Date,
    var status: String?
)
