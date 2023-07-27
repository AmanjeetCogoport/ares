package com.cogoport.ares.model.payment.request
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AccUtilizationRequest(
    var documentNo: Long,
    var entityCode: Int,
    var orgSerialId: Long,
    var sageOrganizationId: String?,
    // Trader partner details id
    var organizationId: UUID?,
    // Organization id of customer/service provider
    var taggedOrganizationId: UUID?,
    var tradePartyMappingId: UUID?,
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
    var taxableAmount: BigDecimal?,
    var tdsAmount: BigDecimal?,
    var tdsAmountLoc: BigDecimal?,
    var zoneCode: String?,
    var docStatus: DocumentStatus?,
    var docValue: String?,
    var dueDate: Date?,
    var transactionDate: Date?,
    var serviceType: ServiceType?,
    var category: String?,
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp? = Timestamp.from(Instant.now()),
    var performedBy: UUID? = null,
    var performedByType: String? = null,
    var migrated: Boolean?,
    var isVoid: Boolean? = false,
    val tagBillIds: List<Long>? = null,
    var settlementEnabled: Boolean? = false,
    var isProforma: Boolean? = false
)
