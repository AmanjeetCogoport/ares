package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
data class AccountPayablesFile(
    var entityCode: Int,
    var orgSerialId: Long,
    var sageOrganizationId: String?,
    var organizationId: UUID?,
    var organizationName: String?,
    var documentNo: Long,
    var documentValue: String,
    var zoneCode: String, // TODO : Create enum for zone , NORTH , SOUTH ,EAST , WEST
    var serviceType: ServiceType,
    var category: String, // TODO : Create enum for Category , ASSET , NON_ASSET
    var accCode: Int,
    var accMode: String,
    var accType: String,
    var signFlag: Short,
    var currency: String, // Currency of amount paid against invoice
    var currencyAmount: BigDecimal, // Amount paid in that currency
    var ledgerCurrency: String, // Ledger currency if amount paid against invoice
    var ledgerAmount: BigDecimal, // Amount paid in ledger currency
    var paymentMode: String?,
    var narration: String?,
    var accountNo: String?,
    var transRefNumber: String,
    var transactionDate: Timestamp,
    var isPosted: Boolean,
    var documentStatus: DocumentStatus // final,proforma,cancelled
)
