package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AccountPayablesFile(
    var entityCode: Int,
    var organizationId: UUID?,
    // Organization id of customer/service provider
    var taggedOrganizationId: UUID?,
    var tradePartyMappingId: UUID?,
    var orgSerialId: Long?,
    var organizationName: String?,
    var documentNo: Long,
    var documentValue: String,
    var zoneCode: ZoneCode,
    var serviceType: ServiceType,
    var category: String, // TODO : Create enum for Category , ASSET , NON_ASSET
    var accMode: AccMode,
    var accType: AccountType,
    var signFlag: Short,
    var bankId: UUID?,
    var currency: String, // Currency of amount paid against invoice
    var currencyAmount: BigDecimal, // Amount paid in that currency
    var ledgerCurrency: String, // Ledger currency if amount paid against invoice
    var ledgerAmount: BigDecimal, // TDS Amount against payment ledger currency
    var paymentMode: PayMode,
    var narration: String?,
    var cogoAccountNo: String?,
    var refAccountNo: String?,
    var transRefNumber: String,
    var transactionDate: Timestamp,
    var bankName: String?,
    var isPosted: Boolean,
    var createdBy: UUID?,
    var updatedBy: UUID?,
    var performedByType: String?,
    var paymentDocumentStatus: PaymentDocumentStatus? = PaymentDocumentStatus.CREATED,
    var currTdsAmount: BigDecimal,
    var ledTdsAmount: BigDecimal
)
