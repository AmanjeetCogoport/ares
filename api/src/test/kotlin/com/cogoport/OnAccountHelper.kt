package com.cogoport

import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DueAmount
import com.cogoport.ares.model.payment.InvoiceStats
import com.cogoport.ares.model.payment.OrgStatsResponse
import com.cogoport.ares.model.payment.OrgStatsResponseForCoeFinance
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.TradePartyOutstanding
import com.cogoport.ares.model.payment.TradePartyOutstandingList
import com.cogoport.ares.model.payment.response.OnAccountTotalAmountResponse
import com.cogoport.ares.model.payment.response.PaymentResponse
import com.cogoport.plutus.model.invoice.GetUserResponse
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.util.UUID

@Singleton
class OnAccountHelper {
    fun getUserResponse(): List<GetUserResponse> {
        return listOf(
            GetUserResponse(
                userEmail = "vivek.garg@cogoport.com",
                userId = UUID.fromString("ec306da2-0d52-4cc1-a7b1-d3a6541f1ce8"),
                userName = "vivekgarg"
            )
        )
    }

    fun getAccountReceivables(payment: Payment): List<PaymentResponse?> {
        return listOf(
            PaymentResponse(
                id = payment.id,
                orgSerialId = 122334,
                bankAccountNumber = "123456789876543",
                entityCode = 301,
                bankName = "rbl",
                accCode = 321000,
                sageOrganizationId = null,
                currency = "INR",
                amount = BigDecimal.valueOf(100.0).setScale(4),
                ledCurrency = "INR",
                ledAmount = BigDecimal.valueOf(100.0).setScale(4),
                updatedBy = null,
                utr = "abhishek_boss",
                accMode = AccMode.AP,
                paymentCode = PaymentCode.PAY,
                paymentDocumentStatus = PaymentDocumentStatus.APPROVED,
                transactionDate = payment.transactionDate,
                uploadedBy = null,
                exchangeRate = BigDecimal.valueOf(1.0).setScale(6),
                paymentNum = 12345,
                paymentNumValue = "PAY12345",
                createdAt = payment.createdAt,
                deletedAt = null,
                narration = null,
                bankId = UUID.fromString("d646dc1c-f366-453c-b56f-2788f36c4136"),
                payMode = PayMode.BANK,
                createdBy = UUID.fromString("ec306da2-0d52-4cc1-a7b1-d3a6541f1ce8"),
                organizationName = "Inext Logistics & Supply Chain Private Limited",
                organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911")
            )
        )
    }

    fun getOrgStatsResponse(): OrgStatsResponse {
        return OrgStatsResponse(
            organizationId = "9f03db0c-88cc-450f-bbb1-38fa31861911",
            receivables = BigDecimal(-60).setScale(4),
            ledgerCurrency = "INR",
            payables = BigDecimal(0)
        )
    }

    fun getOrgStatsResponseForCoeFinance(): OrgStatsResponseForCoeFinance {
        return OrgStatsResponseForCoeFinance(
            organizationId = "9f03db0c-88cc-450f-bbb1-38fa31861911",
            receivables = BigDecimal(100),
            receivablesCurrency = "INR",
            payables = null,
            payablesCurrency = null
        )
    }

    fun getTradePartyOutstandingList(): TradePartyOutstandingList {
        val amountDueList = listOf(
            DueAmount(
                amount = BigDecimal(0),
                invoicesCount = 0,
                currency = "INR"
            )
        )

        val invoiceStats = InvoiceStats(
            invoicesCount = 0,
            invoiceLedAmount = 100.toBigDecimal(),
            amountDue = amountDueList
        )
        return TradePartyOutstandingList(
            list = listOf(
                TradePartyOutstanding(
                    registrationNumber = "DRTPG2189D",
                    currency = "INR",
                    openInvoices = invoiceStats,
                    organizationName = "Inext Logistics & Supply Chain Private Limited",
                    totalOutstanding = invoiceStats,
                    organizationId = "9f03db0c-88cc-450f-bbb1-38fa31861911"
                )
            )
        )
    }

    fun getOnAccountTotalAmountResponse(): OnAccountTotalAmountResponse {
        return OnAccountTotalAmountResponse(
            organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
            accMode = AccMode.AR,
            accType = AccountType.REC, paymentValue = BigDecimal(60.0000).setScale(4)
        )
    }
}
