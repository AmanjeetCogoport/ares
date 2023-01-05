package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.payment.PayMode
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@MappedEntity(value = "suspense_accounts")
data class SuspenseAccount(
    @field:Id @GeneratedValue var id: Long?,
    var paymentId: Long,
    var entityCode: Int,
    var currency: String,
    var amount: BigDecimal,
    var ledCurrency: String?,
    var ledAmount: BigDecimal?,
    var payMode: PayMode?,
    var transRefNumber: String?,
    var refPaymentId: Long?,
    var transactionDate: Timestamp? = Timestamp(System.currentTimeMillis()),
    var isDeleted: Boolean,
    var cogoAccountNo: String?,
    var refAccountNo: String?,
    var bankName: String?,
    var exchangeRate: BigDecimal?,
    var bankId: UUID?,
    var createdBy: UUID?,
    var updatedBy: UUID?,
    var uploadedBy: String?,
    @DateCreated var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    @DateUpdated var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var deletedAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var tradePartyDocument: String?
)
