package com.cogoport.ares.payment.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Column

@MappedEntity(value = "account_utilizations")
data class AccountUtilization (

    @field:Id @GeneratedValue var id: Long?,
    var documentNo:Long,
    var entityCode:Int,
    var entityId:String,
    var orgSerialId:Long,
    var sageOrganizationId:String?,
    var organizationId:String?,
    var organizationName:String?,
    var accCode:Int,
    var accType:String,
    var accMode:String,
    var signFlag:Short,
    var amountCurr: BigDecimal,
    var amountLoc: BigDecimal,
    var payCurr:BigDecimal,
    var payLoc:BigDecimal,
    var dueDate:LocalDate,
    var transactionDate: LocalDate,
    var createdAt: LocalDateTime?,
    var updatedAt: LocalDateTime,
    var zoneCode:String,
    var docStatus:String
)