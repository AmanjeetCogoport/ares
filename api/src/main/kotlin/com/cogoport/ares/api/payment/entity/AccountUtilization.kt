package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.PaymentStatus
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@MappedEntity(value = "account_utilizations")
data class AccountUtilization(
    @field:Id @GeneratedValue var id: Long?,
    var documentNo: Long,
    var documentValue: String?,
    var zoneCode: String?,
    var serviceType: String,
    var documentStatus: DocumentStatus?,
    var entityCode: Int,
    var category: String?,
    var orgSerialId: Long?,
    var sageOrganizationId: String?,
    // Trader partner details id
    var organizationId: UUID?,
    // Organization id of customer/service provider
    var taggedOrganizationId: UUID?,
    var tradePartyMappingId: UUID?,
    var organizationName: String?,
    var accCode: Int,
    var accType: AccountType,
    var accMode: AccMode,
    var signFlag: Short,
    var currency: String = "INR",
    var ledCurrency: String = "INR",
    var amountCurr: BigDecimal,
    var amountLoc: BigDecimal,
    var taxableAmount: BigDecimal? = BigDecimal.ZERO,
    var payCurr: BigDecimal = 0.toBigDecimal(),
    var payLoc: BigDecimal = 0.toBigDecimal(),
    var dueDate: Date?,
    var transactionDate: Date?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp? = Timestamp.from(Instant.now()),
    var migrated: Boolean?
) {
    fun getPaymentStatus(): PaymentStatus {
        if (amountCurr == payCurr) {
            return PaymentStatus.PAID
        } else if (amountCurr - payCurr > 0.toBigDecimal() && payCurr != 0.toBigDecimal()) {
            return PaymentStatus.PARTIAL_PAID
        }
        return PaymentStatus.PAID
    }
}
