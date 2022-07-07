package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.InvoiceStatus
import com.cogoport.ares.model.payment.InvoiceType
import com.cogoport.ares.model.payment.SettlementDocumentRequest
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import java.sql.Timestamp
import kotlin.math.ceil

@Singleton
class SettlementServiceImpl : SettlementService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    override suspend fun getDocuments(request: SettlementDocumentRequest) = getInvoicesFromOpenSearch(request)

    override suspend fun getAccountBalance(request: SummaryRequest): SummaryResponse {
        return SummaryResponse(OpenSearchClient().getSummary(request = request))
    }

    override suspend fun getMatchingBalance(documentIds: List<String>): SummaryResponse {
        return SummaryResponse(OpenSearchClient().getSummary(documentIds = documentIds))
    }

    override suspend fun check(request: CheckRequest): List<CheckDocument> {
        val source = mutableListOf<CheckDocument>()
        val dest = mutableListOf<CheckDocument>()
        val sourceType = mutableListOf<AccountType>()
        val desttype = mutableListOf<AccountType>()
        val creditType = listOf(AccountType.REC, AccountType.PCN, AccountType.PAY)
        val debitType = listOf(AccountType.SINV, AccountType.PINV)
        request.stackDetails.reversed().forEach {
            if (creditType.contains(it.accountType)){
                source.add(it)
                sourceType.add(it.accountType)
            }
            else if (debitType.contains(it.accountType)){
                dest.add(it)
                desttype.add(it.accountType)
            }
        }
        validateInput(sourceType, desttype)
        val settledList = settleDocuments(source, dest)
        return request.stackDetails.map{ r -> settledList.filter { it.id == r.id }[0] }
    }

    private fun settleDocuments(source: MutableList<CheckDocument>, dest: MutableList<CheckDocument>): MutableList<CheckDocument>{
        val response = mutableListOf<CheckDocument>()
        for (payment in source){
            var availableAmount = payment.allocationAmount
            val canSettle = fetchSettlingDocs(payment.accountType)
            for (invoice in dest) {
                if (canSettle.contains(invoice.accountType)) {
                    availableAmount = doSettlement(invoice, availableAmount, payment, response, source)
                }
            }
            assignStatus(payment)
            payment.allocationAmount -= availableAmount
            payment.balanceAfterAllocation = payment.balanceAmount - payment.allocationAmount
                    response.add(payment)
        }
        return response
    }

    private fun doSettlement(invoice: CheckDocument, availableAmount: BigDecimal, payment: CheckDocument, response: MutableList<CheckDocument>, source: MutableList<CheckDocument>): BigDecimal {
        var amount = availableAmount
        var rate = 1.toBigDecimal()
        if (payment.currency != invoice.currency){
            rate = getExchangeRate(payment.currency, invoice.currency, payment.transactionDate)
            amount = getExchangeValue(availableAmount, rate)
        }
        val toSettleAmount = invoice.allocationAmount - invoice.settledAmount
        if (toSettleAmount != 0.0.toBigDecimal()) {
            if (amount == toSettleAmount) {
                amount = updateDocuments(invoice, payment, toSettleAmount, amount, rate)
                invoice.allocationAmount = invoice.settledAmount
                invoice.balanceAfterAllocation = invoice.balanceAmount - invoice.allocationAmount
                response.add(invoice)
            } else if (amount > toSettleAmount) {
                amount = updateDocuments(invoice, payment, toSettleAmount, amount, rate)
                invoice.allocationAmount = invoice.settledAmount
                invoice.balanceAfterAllocation = invoice.balanceAmount - invoice.allocationAmount
                response.add(invoice)
            } else if (amount < toSettleAmount) {
                amount = updateDocuments(invoice, payment, amount, amount, rate)
                if (payment == source.last()) {
                    invoice.allocationAmount = invoice.settledAmount
                    invoice.balanceAfterAllocation = invoice.balanceAmount - invoice.allocationAmount
                    response.add(invoice)
                }
            }
        }
        return getExchangeValue(amount, rate, true)
    }

    private fun getExchangeValue(amount: BigDecimal, exchangeRate: BigDecimal, reverse: Boolean = false):BigDecimal{
        return if (reverse){
            amount / exchangeRate
        } else{
            amount * exchangeRate
        }
    }

    private fun getExchangeRate(from: String, to: String, transactionDate: Timestamp): BigDecimal{
        return 79.toBigDecimal()
    }
    private fun fetchSettlingDocs(accType: AccountType): List<AccountType>{
        return when (accType) {
            AccountType.REC -> { listOf(AccountType.SINV, AccountType.SDN) }
            AccountType.PINV -> { listOf(AccountType.PAY, AccountType.PCN) }
            AccountType.PCN -> { listOf(AccountType.PINV, AccountType.PDN) }
            AccountType.PAY -> { listOf(AccountType.PINV, AccountType.PDN) }
            AccountType.SINV -> { listOf(AccountType.REC, AccountType.SCN) }
            else -> { emptyList() }
        }
    }

    private fun validateInput(source: List<AccountType>, dest: List<AccountType>){
        var creditCount = 0
        var debitCount = 0
        for (invoice in dest){
            fetchSettlingDocs(invoice).forEach { if (source.contains(it)) creditCount += 1 }
            if (creditCount == 0) throw AresException(AresError.ERR_1501, "")
        }
        for (payment in source){
            fetchSettlingDocs(payment).forEach { if (dest.contains(it)) debitCount += 1 }
            if (debitCount == 0) throw AresException(AresError.ERR_1502, "")
        }
    }

    private fun updateDocuments(invoice: CheckDocument, payment: CheckDocument, toSettleAmount: BigDecimal, availableAmount: BigDecimal, exchangeRate: BigDecimal): BigDecimal{
        val amount = availableAmount - toSettleAmount
        invoice.settledAmount += toSettleAmount
        payment.settledAmount += getExchangeValue(toSettleAmount, exchangeRate, true)
        assignStatus(invoice)
        assignStatus(payment)
        return getExchangeValue(amount, exchangeRate, true)
    }

    private fun assignStatus(doc: CheckDocument){
        if (doc.balanceAmount == doc.settledAmount){
            doc.documentStatus = InvoiceStatus.KNOCKED_OFF
        }
        else if (doc.settledAmount == 0.0.toBigDecimal()){
            doc.documentStatus = InvoiceStatus.UNPAID
        }
        else if (doc.balanceAmount > doc.settledAmount){
            doc.documentStatus = InvoiceStatus.PARTIAL_PAID
        }
    }

    private fun getInvoicesFromOpenSearch(request: SettlementDocumentRequest): ResponseList<Document> {
        val clientResponse = OpenSearchClient().getSettlementInvoices(request)
        val total = clientResponse?.hits()?.total()?.value() ?: 0
        val accountUtilization = invoiceListResponses(clientResponse)
        return ResponseList(
            list = accountUtilization,
            totalPages = ceil(total.toDouble() / request.pageLimit).toLong(),
            totalRecords = total,
            pageNo = request.page
        )
    }

    private fun invoiceListResponses(clientResponse: SearchResponse<AccountUtilizationResponse>?): List<Document>? {
        val data = clientResponse?.hits()?.hits()?.map {
            val response = it.source()
            val tds = response?.amountCurr!! * AresConstants.TWO_PERCENT.toBigDecimal()
            val afterTdsAmount = response.amountCurr!! - tds
            val settledAmount = response.payCurr!!
            val balanceAmount = afterTdsAmount - settledAmount
            val status = getInvoiceStatus(afterTdsAmount, balanceAmount) // should come from index
            TODO("add taxable amount to account utilization index from plutus")
            TODO("add status column in account utilizations")
            Document(
                id = response.id,
                documentNo = response.documentValue,
                documentType = getInvoiceType(response.accType!!),
                invoiceDate = response.transactionDate,
                dueDate = response.dueDate,
                invoiceAmount = response.amountCurr,
                taxableAmount = response.amountCurr,
                tds = tds,
                afterTdsAmount = afterTdsAmount,
                settledAmount = settledAmount,
                balanceAmount = balanceAmount,
                invoiceStatus = status
            )
        }
        return data
    }

    private fun getInvoiceStatus(afterTdsAmount: BigDecimal, balanceAmount: BigDecimal): InvoiceStatus {
        return if (balanceAmount == 0.toBigDecimal()) {
            InvoiceStatus.PAID
        } else if (afterTdsAmount == balanceAmount) {
            InvoiceStatus.UNPAID
        } else {
            InvoiceStatus.PARTIAL_PAID
        }
    }

    private fun getInvoiceType(accType: AccountType): InvoiceType {
        return when (accType) {
            AccountType.SINV -> { InvoiceType.SALES_INVOICES }
            AccountType.SCN -> { InvoiceType.PURCHASE_CREDIT_NOTE }
            AccountType.SDN -> { InvoiceType.SALES_DEBIT_NOTE }
            AccountType.REC -> { InvoiceType.SALES_PAYMENT }
            AccountType.PINV -> { InvoiceType.PURCHASE_INVOICES }
            AccountType.PCN -> { InvoiceType.PURCHASE_CREDIT_NOTE }
            AccountType.PDN -> { InvoiceType.PURCHASE_DEBIT_NOTE }
            AccountType.PAY -> { InvoiceType.PURCHASE_PAYMENT }
            else -> { TODO("Not Decided Yet") }
        }
    }
}
