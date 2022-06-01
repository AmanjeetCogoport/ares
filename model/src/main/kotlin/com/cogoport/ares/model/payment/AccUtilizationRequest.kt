package com.cogoport.ares.model.payment
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

data class AccUtilizationRequest(
    var documentNo: Long,
    var entityCode: Int,
    var entityId: String,
    var orgSerialId: Long,
    var sageOrganizationId: String?,
    var organizationId: UUID?,
    var organizationName: String?,
    var accCode: Int,
    var accType: String,
    var accMode: String,
    var signFlag: Int,
    var currencyAmount: BigDecimal,
    var ledgerAmount: BigDecimal,
    var currencyPayment: BigDecimal,
    var ledgerPayment: BigDecimal,
    var zoneCode: String,
    var docStatus: String,
    var docValue: String?,
    var dueDate: Timestamp?,
    var transactionDate: Timestamp?
)
