package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity(value = "account_utilizations")
data class AccountUtilization(
    @field:Id @GeneratedValue var id: Long?,
    var entityCode: Int,
    var documentNo: Long,
    var orgSerialId: Long,
    var organizationId: UUID,
    var organizationName: String?,
    var sageOrganizationId: String?,
    var accCode: Int,
    var accType: AccountType,
    var accMode: AccMode,
    var signFlag: Int,
    var amountCurr: BigDecimal,
    var amountLoc: BigDecimal,
    var payCurr: BigDecimal = 0.toBigDecimal(),
    var payLoc: BigDecimal = 0.toBigDecimal(),
    var dueDate: LocalDate,
    var transactionDate: LocalDate,
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    var modifiedAt: LocalDateTime? = LocalDateTime.now(),
    var zoneCode: String,
    var docStatus: DocStatus,
    var docValue: String?,
    var serviceType: ServiceType,
    var currency: String,
    var ledCurrency: String,
    var category: String
)
