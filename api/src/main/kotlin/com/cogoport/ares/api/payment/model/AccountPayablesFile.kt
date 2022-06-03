package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.ZoneCode
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
data class AccountPayablesFile(
    var entityCode: Int,
    var organizationId: UUID?,
    var organizationName: String?,
    var documentNo: Long,
    var documentValue: String,
    var zoneCode: ZoneCode,
    var serviceType: ServiceType,
    var category: String, // TODO : Create enum for Category , ASSET , NON_ASSET
    var accMode: AccMode,
    var accType: AccountType,
    var signFlag: Short,
    var currency: String, // Currency of amount paid against invoice
    var currencyAmount: BigDecimal, // Amount paid in that currency
    var ledgerCurrency: String, // Ledger currency if amount paid against invoice
    var ledgerAmount: BigDecimal, // Amount paid in ledger currency
    var currTdsAmount: BigDecimal, // TDS Amount against payment pay currency
    var ledTdsAmount: BigDecimal, // TDS Amount against payment ledger currency
    var paymentMode: PayMode,
    var narration: String?,
    var cogoAccountNo: String?,
    var refAccountNo: String?,
    var transRefNumber: String,
    var transactionDate: Timestamp,
    var bankName: String?,
    var isPosted: Boolean
)
