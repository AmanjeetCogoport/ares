package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.Date

@MappedEntity(value = "account_utilizations")
data class AccountUtilization(
    @field:Id @GeneratedValue var id: Long?,
    var documentNo: Long,
    var documentValue: String?,
    var zoneCode: String,
    var serviceType: String,
    var documentStatus: DocumentStatus,
    var entityCode: Int,
    var category: String,
    var orgSerialId: Long,
    var sageOrganizationId: String?,
    var organizationId: UUID,
    var organizationName: String?,
    var accCode: Int,
    var accType: AccountType,
    var accMode: AccMode,
    var signFlag: Short,
    var currency: String,
    var ledCurrency: String,
    var amountCurr: BigDecimal,
    var amountLoc: BigDecimal,
    var payCurr: BigDecimal = 0.toBigDecimal(),
    var payLoc: BigDecimal = 0.toBigDecimal(),
    var dueDate: Date,
    var transactionDate: Date,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp? = Timestamp.from(Instant.now()),
)
