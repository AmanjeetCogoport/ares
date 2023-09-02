package com.cogoport.ares.api.payment.entity

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import javax.persistence.Transient

@MappedEntity
data class EntityLevelStats(
    val invoiceNotDueAmount: BigDecimal?,
    val invoiceTodayAmount: BigDecimal?,
    val invoiceThirtyAmount: BigDecimal?,
    val invoiceSixtyAmount: BigDecimal?,
    val invoiceNinetyAmount: BigDecimal?,
    val invoiceOneEightyAmount: BigDecimal?,
    val invoiceThreeSixtyFiveAmount: BigDecimal?,
    val invoiceThreeSixtyFivePlusAmount: BigDecimal?,
    val invoiceNotDueCount: Long?,
    val invoiceTodayCount: Long?,
    val invoiceThirtyCount: Long?,
    val invoiceSixtyCount: Long?,
    val invoiceNinetyCount: Long?,
    val invoiceOneEightyCount: Long?,
    val invoiceThreeSixtyFiveCount: Long?,
    val invoiceThreeSixtyFivePlusCount: Long?,
    val onAccountNotDueAmount: BigDecimal?,
    val onAccountTodayAmount: BigDecimal?,
    val onAccountThirtyAmount: BigDecimal?,
    val onAccountSixtyAmount: BigDecimal?,
    val onAccountNinetyAmount: BigDecimal?,
    val onAccountOneEightyAmount: BigDecimal?,
    val onAccountThreeSixtyFiveAmount: BigDecimal?,
    val onAccountThreeSixtyFivePlusAmount: BigDecimal?,
    val onAccountNotDueCount: Long?,
    val onAccountTodayCount: Long?,
    val onAccountThirtyCount: Long?,
    val onAccountSixtyCount: Long?,
    val onAccountNinetyCount: Long?,
    val onAccountOneEightyCount: Long?,
    val onAccountThreeSixtyFiveCount: Long?,
    val onAccountThreeSixtyFivePlusCount: Long?,
    val notDueOutstanding: BigDecimal?,
    val todayOutstanding: BigDecimal?,
    val thirtyOutstanding: BigDecimal?,
    val sixtyOutstanding: BigDecimal?,
    val ninetyOutstanding: BigDecimal?,
    val oneEightyOutstanding: BigDecimal?,
    val threeSixtyFiveOutstanding: BigDecimal?,
    val threeSixtyFivePlusOutstanding: BigDecimal?,
    val totalOpenInvoiceAmount: BigDecimal?,
    val totalOpenOnAccountAmount: BigDecimal?,
    val totalOutstanding: BigDecimal?,
    val ledCurrency: String?,
    val entityCode: Int?
) {
    @field:Transient
    var totalOpenInvoiceCount: Long? = null
    @field:Transient
    var totalOpenOnAccountCount: Long? = null
}
