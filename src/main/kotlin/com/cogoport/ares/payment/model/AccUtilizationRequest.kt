package com.cogoport.ares.payment.model
import java.math.BigDecimal

data class AccUtilizationRequest (
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
    var currencyAmount:BigDecimal,
    var ledgerAmount:BigDecimal,
    var currencyPayment:BigDecimal,
    var ledgerPayment:BigDecimal,
    var zoneCode:String,
    var docStatus:String
)