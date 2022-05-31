package com.cogoport.ares.model.payment
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
data class AccUtilizationRequest(
    var documentNo: Long,
    var entityCode: Int,
    var orgSerialId: Long,
    var sageOrganizationId: String?,
    var organizationId: UUID?,
    var organizationName: String?,
    var accCode: Int,
    var accType: String,
    var accMode: String,
    var signFlag: Short,
    var currency: String,
    var ledCurrency: String,
    var currencyAmount: BigDecimal,
    var ledgerAmount: BigDecimal,
    var currencyPayment: BigDecimal,
    var ledgerPayment: BigDecimal,
    var zoneCode: String,
    var docStatus: String,
    var docValue: String?,
    var dueDate: Timestamp?,
    var transactionDate: Timestamp?,
    var serviceType: String?,
    var category: String?
)
