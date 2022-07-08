package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
import com.cogoport.ares.api.settlement.mapper.SettlementMapper
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.InvoiceStatus
import com.cogoport.ares.model.payment.InvoiceType
import com.cogoport.ares.model.settlement.SettlementDocumentRequest
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.SettledDocument
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import java.sql.Timestamp
import kotlin.math.ceil
import kotlin.math.roundToInt

@Singleton
class SettlementServiceImpl : SettlementService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var historyDocumentConverter: HistoryDocumentMapper

    @Inject
    lateinit var settlementConvert: SettlementMapper

    @Inject
    lateinit var paymentRepository: PaymentRepository

    override suspend fun getDocuments(request: SettlementDocumentRequest) = getInvoicesFromOpenSearch(request)

    override suspend fun getAccountBalance(request: SummaryRequest): SummaryResponse {
        return SummaryResponse(OpenSearchClient().getSummary(request = request))
    }

    override suspend fun getMatchingBalance(documentIds: List<String>): SummaryResponse {
        return SummaryResponse(OpenSearchClient().getSummary(documentIds = documentIds))
    }

    /**
     * Get History Document list (Credit Notes and On Account Payments)
     * @param request
     * @return ResponseList<HistoryDocument>
     */
    override suspend fun getHistory(request: SettlementHistoryRequest): ResponseList<HistoryDocument?> {
        val documents = accountUtilizationRepository.getHistoryDocument(request.orgId, request.settlementType, request.page, request.pageLimit)
        val totalRecords = accountUtilizationRepository.countHistoryDocument(request.orgId)
        var historyDocuments = mutableListOf<HistoryDocument>()
        documents?.forEach { doc -> historyDocuments.add(historyDocumentConverter.convertToModel(doc)) }
        return ResponseList(
            list = historyDocuments,
            totalPages = getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    /**
     *
     */
    private fun getTotalPages(totalRows: Long, pageSize: Int): Long {

        return try {
            val totalPageSize = if (pageSize > 0) pageSize else 1
            ceil((totalRows.toFloat() / totalPageSize.toFloat()).toDouble()).roundToInt().toLong()
        } catch (e: Exception) {
            0
        }
    }

    /**
     *
     */
    override suspend fun getSettlement(request: SettlementRequest): ResponseList<SettledDocument?> {
        var settledDocuments = mutableListOf<SettledDocument>()
        var settlements = mutableListOf<Settlement>()
        when (request.accType) {
            AccountType.REC, AccountType.PCN -> {
                settlements = settlementRepository.findSettlement(
                    request.documentNo,
                    mutableListOf(SettlementType.REC, SettlementType.PCN),
                    request.page,
                    request.pageLimit
                ) as MutableList<Settlement>
            }
        }

        var totalRecords = settlementRepository.countSettlement(request.documentNo, mutableListOf(
            SettlementType.REC, SettlementType.PCN
        ) )


        settlements?.forEach {
            settlement ->
            when (request.accType) {
                AccountType.REC, AccountType.PCN -> {
                    settledDocuments.add(settlementConvert.convertSourceToSettlementDocument(settlement))
                }
            }
        }
        return ResponseList(
            list = settledDocuments,
            totalPages = getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    override suspend fun check(request: CheckRequest): List<CheckDocument> {
        val source = mutableListOf<CheckDocument>()
        val dest = mutableListOf<CheckDocument>()
        val creditType = listOf(AccountType.REC, AccountType.PCN, AccountType.PAY, AccountType.SCN)
        val debitType = listOf(AccountType.SINV, AccountType.PINV, AccountType.SDN, AccountType.PDN)
        for (doc in request.stackDetails.reversed()) {
            if (creditType.contains(doc.accountType)) source.add(doc) else if (debitType.contains(doc.accountType)) dest.add(doc)
        }
        if (source.isEmpty() && dest.map { it.accountType }.contains(AccountType.SINV) && dest.map { it.accountType }.contains(AccountType.PINV)) {
            dest.filter { it.accountType == AccountType.SINV }.forEach {
                source.add(it)
                dest.remove(it)
            }
        }
        validateInput(source, dest)
        val settledList = settleDocuments(source, dest)
        return request.stackDetails.map { r -> settledList.filter { it.id == r.id }[0] }
    }

    private suspend fun settleDocuments(source: MutableList<CheckDocument>, dest: MutableList<CheckDocument>): MutableList<CheckDocument> {
        val response = mutableListOf<CheckDocument>()
        for (payment in source) {
            var availableAmount = payment.allocationAmount
            val canSettle = fetchSettlingDocs(payment.accountType)
            for (invoice in dest) {
                if (canSettle.contains(invoice.accountType)) {
                    availableAmount = doSettlement(invoice, availableAmount, payment, response, source)
                }
            }
            payment.allocationAmount -= availableAmount
            payment.balanceAfterAllocation = payment.balanceAmount.subtract(payment.allocationAmount)
            assignStatus(payment)
            response.add(payment)
        }
        return response
    }

    private suspend fun doSettlement(invoice: CheckDocument, availableAmount: BigDecimal, payment: CheckDocument, response: MutableList<CheckDocument>, source: MutableList<CheckDocument>): BigDecimal {
        var amount = availableAmount
        val toSettleAmount = invoice.allocationAmount - invoice.settledAmount
        if (toSettleAmount != 0.0.toBigDecimal()) {
            var rate = 1.toBigDecimal()
            if (payment.currency != invoice.currency) {
                rate = if (payment.legCurrency == invoice.currency) {
                    paymentRepository.findByPaymentId(payment.id)?.exchangeRate ?: getExchangeRate(payment.currency, invoice.currency, payment.transactionDate)
                } else {
                    getExchangeRate(payment.currency, invoice.currency, payment.transactionDate)
                }
                amount = getExchangeValue(availableAmount, rate)
            }
            if (amount >= toSettleAmount) {
                amount = updateDocuments(invoice, payment, response, toSettleAmount, amount, rate, true)
            } else if (amount < toSettleAmount) {
                var pushDoc = false
                if (payment == source.last()) pushDoc = true
                amount = updateDocuments(invoice, payment, response, amount, amount, rate, pushDoc)
            }
        }
        return amount
    }

    private fun updateDocuments(invoice: CheckDocument, payment: CheckDocument, response: MutableList<CheckDocument>, toSettleAmount: BigDecimal, availableAmount: BigDecimal, exchangeRate: BigDecimal, pushDoc: Boolean): BigDecimal {
        val amount = availableAmount - toSettleAmount
        invoice.settledAmount += toSettleAmount
        payment.settledAmount += getExchangeValue(toSettleAmount, exchangeRate, true)
        assignStatus(invoice)
        assignStatus(payment)
        if (pushDoc) {
            invoice.allocationAmount = invoice.settledAmount
            invoice.balanceAfterAllocation = invoice.balanceAmount.subtract(invoice.allocationAmount)
            response.add(invoice)
        }
        return getExchangeValue(amount, exchangeRate, true)
    }

    private fun getExchangeValue(amount: BigDecimal, exchangeRate: BigDecimal, reverse: Boolean = false): BigDecimal {
        return if (reverse) {
            amount / exchangeRate
        } else {
            amount * exchangeRate
        }
    }

    private fun getExchangeRate(from: String, to: String, transactionDate: Timestamp): BigDecimal {
        return 0.012.toBigDecimal()
    }
    private fun fetchSettlingDocs(accType: AccountType): List<AccountType> {
        return when (accType) {
            AccountType.REC -> { listOf(AccountType.SINV, AccountType.SDN) }
            AccountType.PINV -> { listOf(AccountType.PAY, AccountType.PCN, AccountType.SINV) }
            AccountType.PCN -> { listOf(AccountType.PINV, AccountType.PDN) }
            AccountType.PAY -> { listOf(AccountType.PINV, AccountType.PDN) }
            AccountType.SINV -> { listOf(AccountType.REC, AccountType.SCN, AccountType.PINV) }
            AccountType.SCN -> { listOf(AccountType.SINV, AccountType.SDN) }
            AccountType.SDN -> { listOf(AccountType.SCN, AccountType.REC) }
            AccountType.PDN -> { listOf(AccountType.PCN, AccountType.PAY) }
            else -> { emptyList() }
        }
    }

    private fun validateInput(source: MutableList<CheckDocument>, dest: MutableList<CheckDocument>) {
        var creditCount = 0
        var debitCount = 0
        for (payment in source) {
            fetchSettlingDocs(payment.accountType).forEach { debit -> if (dest.map { it.accountType }.contains(debit)) debitCount += 1 }
            if (debitCount == 0) throw AresException(AresError.ERR_1502, "") else debitCount = 0
        }
        for (invoice in dest) {
            fetchSettlingDocs(invoice.accountType).forEach { credit -> if (source.map { it.accountType }.contains(credit)) creditCount += 1 }
            if (creditCount == 0) throw AresException(AresError.ERR_1501, "") else creditCount = 0
        }
    }

    private fun assignStatus(doc: CheckDocument) {
        if (doc.balanceAmount.compareTo(doc.settledAmount) == 0) {
            doc.documentStatus = InvoiceStatus.KNOCKED_OFF
        } else if (doc.settledAmount.compareTo(0.toBigDecimal()) == 0) {
            doc.documentStatus = InvoiceStatus.UNPAID
        } else if (doc.balanceAmount.compareTo(doc.settledAmount) == 1) {
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
