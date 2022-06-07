package com.cogoport.ares.model.payment
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@Introspected
data class AccUtilizationRequest(
    var documentNo: Long,
    var entityCode: Int,
    var orgSerialId: Long,
    var sageOrganizationId: String?,
    var organizationId: UUID?,
    var organizationName: String?,
    var accType: AccountType?,
    var accMode: AccMode,
    var signFlag: Short,
    var currency: String,
    var ledCurrency: String,
    var currencyAmount: BigDecimal?,
    var ledgerAmount: BigDecimal?,
    var currencyPayment: BigDecimal?,
    var ledgerPayment: BigDecimal?,
    var zoneCode: String?,
    var docStatus: DocumentStatus?,
    var docValue: String?,
    var dueDate: Date?,
    var transactionDate: Date?,
    var serviceType: String?,
    var category: String?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp? = Timestamp.from(Instant.now())
)
