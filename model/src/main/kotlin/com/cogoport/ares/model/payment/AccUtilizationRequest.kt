package com.cogoport.ares.model.payment

import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

data class AccUtilizationRequest(
    var documentNo: Long,
    var docValue: String?,
    var zoneCode: String,
    var entityCode: Int,
    var entityId: String,
    var orgSerialId: Long,
    var organizationId: UUID,
    var organizationName: String?,
    var sageOrganizationId: String?,
    var accCode: Int,
    var accType: String,
    var accMode: String,
    var signFlag: Int,
    var currencyAmount: BigDecimal,
    var ledgerAmount: BigDecimal,
    var currencyPayment: BigDecimal,
    var ledgerPayment: BigDecimal,
    var dueDate: Timestamp,
    var transactionDate: Timestamp,
    var docStatus: String
)
