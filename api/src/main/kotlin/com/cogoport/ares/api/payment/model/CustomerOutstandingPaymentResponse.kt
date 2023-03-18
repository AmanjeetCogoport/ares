package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.DocStatus
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date

@Introspected
@MappedEntity
data class CustomerOutstandingPaymentResponse(

    val accCode: Int?,
    val accMode: AccMode?,
    val paymentAmount: String?,
    val documentNo: Long?,
    val amountLoc: BigDecimal?,
    val utilizedAmount: BigDecimal?,
    val paymentLoc: BigDecimal?,
    val currency: String?,
    val entityCode: Int?,
    val ledgerCurrency: String?,
    val organizationName: String?,
    val paymentNumber: String?,
    val signFlag: Short?,
    val transactionDate: Date?,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
    val utilizationStatus: DocStatus?
)
