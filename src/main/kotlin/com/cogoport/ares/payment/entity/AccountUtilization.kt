package com.cogoport.ares.payment.entity


import com.cogoport.ares.common.enums.AccMode
import com.cogoport.ares.payment.model.AccountType
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
    var entityId: UUID,
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
    var zoneCode:String,
    var docStatus:String,
    var transactionDate: LocalDate,
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
