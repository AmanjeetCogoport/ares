package com.cogoport

import com.cogoport.ares.api.common.models.ARLedgerJobDetailsResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.common.TradePartyOutstandingRes
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.response.CreditDebitBalance
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@Singleton
class AccountUtilizationHelper {
    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    fun getARLedgerJobDetailsResponse(): List<ARLedgerJobDetailsResponse> {
        return listOf(
            ARLedgerJobDetailsResponse(
                transactionDate = "2023-08-01",
                documentType = "SINV",
                documentNumber = "COGO1234",
                currency = "INR",
                amount = "15000",
                debit = 15000.toBigDecimal(),
                credit = BigDecimal.ZERO,
                unutilizedAmount = 7500.toBigDecimal(),
                transactionRefNumber = null,
                jobDocuments = mutableListOf(),
                shipmentDocumentNumber = "1234qwerty",
                houseDocumentNumber = ""
            )
        )
    }
    fun getOpeningAndClosingLedger(): CreditDebitBalance {
        return CreditDebitBalance(
            ledgerCurrency = "INR",
            credit = 11345664.toBigDecimal(),
            debit = 11021867.toBigDecimal()
        )
    }

    suspend fun saveAccountUtil() {
        val accUtil = AccountUtilization(
            id = null,
            documentNo = 113121115,
            documentValue = null,
            zoneCode = null,
            serviceType = ServiceType.LCL_FREIGHT.name,
            documentStatus = DocumentStatus.FINAL,
            entityCode = 301,
            category = null,
            orgSerialId = 25440,
            sageOrganizationId = "",
            organizationId = UUID.fromString("9b92503b-6374-4274-9be4-e83a42fc35fe"),
            taggedOrganizationId = UUID.fromString("1e3ed3f5-da62-4c81-bbab-7608fdac892d"),
            tradePartyMappingId = null,
            organizationName = null,
            accCode = 223000,
            accType = AccountType.SINV,
            accMode = AccMode.AR,
            signFlag = 1,
            currency = "INR",
            ledCurrency = "INR",
            amountCurr = 400.toBigDecimal(),
            amountLoc = 400.toBigDecimal(),
            taxableAmount = 333.toBigDecimal(),
            payCurr = 0.toBigDecimal(),
            payLoc = 0.toBigDecimal(),
            dueDate = Date(),
            transactionDate = Date(),
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now()),
            migrated = false,
            isVoid = false,
            taggedBillId = null,
            tdsAmountLoc = 0.toBigDecimal(),
            tdsAmount = 0.toBigDecimal(),
            settlementEnabled = true,
            deletedAt = null,
            isProforma = null
        )
        accountUtilizationRepo.save(accUtil)
    }

    fun getOrganizationTradePartyOutstandingResponse(): List<TradePartyOutstandingRes> {
        return listOf(
            TradePartyOutstandingRes(
                tradePartyDetailId = UUID.fromString("9b92503b-6374-4274-9be4-e83a42fc35fe"),
                openInvoicesCount = 1,
                openInvoicesLedAmount = 400.toBigDecimal().setScale(4),
                overdueOpenInvoicesLedAmount = 0.toBigDecimal(),
                outstandingLedAmount = 400.toBigDecimal().setScale(4),
                entityCode = 301,
                ledCurrency = "INR",
                registrationNumber = "AADCS3124K"
            )
        )
    }

    fun getOverallStatsForMultipleConsumersTest(): ResponseList<StatsForCustomerResponse> {
        val invoiceDetails = StatsForCustomerResponse(
            organizationId = UUID.fromString("1e3ed3f5-da62-4c81-bbab-7608fdac892d"),
            totalProformaAmount = 0.toBigDecimal(),
            proformaInvoicesCount = 0,
            totalDueAmount = 0.toBigDecimal(),
            dueInvoicesCount = 0,
            totalOverdueAmount = 400.0000.toBigDecimal().setScale(4),
            overdueInvoicesCount = 1,
            totalAmountReceivables = 400.0000.toBigDecimal().setScale(4),
            receivablesInvoicesCount = 1,
            onAccountPayment = 0.toBigDecimal(),
            dueByThirtyDaysAmount = 400.0000.toBigDecimal().setScale(4),
            dueBySixtyDaysAmount = 0.toBigDecimal(),
            dueByNinetyDaysAmount = 0.toBigDecimal(),
            dueByNinetyPlusDaysAmount = 0.toBigDecimal(),
            dueByThirtyDaysCount = 1,
            dueBySixtyDaysCount = 0,
            dueByNinetyDaysCount = 0,
            dueByNinetyPlusDaysCount = 0
        )
        val response = ResponseList<StatsForCustomerResponse>()
        response.list = listOf(invoiceDetails)
        return response
    }
}
