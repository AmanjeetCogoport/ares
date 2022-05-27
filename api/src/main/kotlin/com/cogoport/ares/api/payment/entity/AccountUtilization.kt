package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@MappedEntity(value = "account_utilizations")
data class AccountUtilization(
    @field:Id @GeneratedValue var id: Long?,
    var documentNo: Long,
    var documentValue: String?,
    var zoneCode: String,
    var serviceType: String? = "",
    var docStatus: String,
    var entityCode: Int,
    var category: String? = "",
    var orgSerialId: Long,
    var sageOrganizationId: String?,
    var organizationId: UUID?,
    var organizationName: String?,
    var accCode: Int,
    var accType: AccountType,
    var accMode: AccMode,
    var signFlag: Int,
    val currency: String = "INR",
    val ledCurrency: String = "INR",
    var amountCurr: BigDecimal,
    var amountLoc: BigDecimal,
    var payCurr: BigDecimal = 0.toBigDecimal(),
    var payLoc: BigDecimal = 0.toBigDecimal(),
    var dueDate: Timestamp? = Timestamp(System.currentTimeMillis()),
    var transactionDate: Timestamp? = Timestamp(System.currentTimeMillis()),
    var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
    var modifiedAt: Timestamp? = Timestamp(System.currentTimeMillis())
)
