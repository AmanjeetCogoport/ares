package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
import com.cogoport.ares.api.settlement.mapper.SettledInvoiceMapper
import com.cogoport.ares.api.settlement.mapper.SettlementMapper
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.InvoiceStatus
import com.cogoport.ares.model.payment.InvoiceType
import com.cogoport.ares.model.payment.Operator
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.SettlementDocumentRequest
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
import java.time.Instant
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
    lateinit var settledInvoiceConverter: SettledInvoiceMapper

    @Inject
    lateinit var paymentRepository: PaymentRepository

    override suspend fun getDocuments(request: SettlementDocumentRequest) = getInvoicesFromOpenSearch(request)

    /**
     * Get Account balance of selected Business Partners.
     * @param SummaryRequest
     * @return SummaryResponse
     */
    override suspend fun getAccountBalance(request: SummaryRequest): SummaryResponse {
        return SummaryResponse(OpenSearchClient().getSummary(request = request))
    }

    /**
     * Get Matching balance of selected records.
     * @param documentIds
     * @return SummaryResponse
     */
    override suspend fun getMatchingBalance(documentIds: List<String>): SummaryResponse {
        return SummaryResponse(OpenSearchClient().getSummary(documentIds = documentIds))
    }

    /**
     * Get History Document list (Credit Notes and On Account Payments).
     * @param request
     * @return ResponseList<HistoryDocument>
     */
    override suspend fun getHistory(request: SettlementHistoryRequest): ResponseList<HistoryDocument?> {
        var accountTypes = stringAccountTypes(request)
        val documents = accountUtilizationRepository.getHistoryDocument(request.orgId, accountTypes, request.page, request.pageLimit, request.startDate, request.endDate)
        val totalRecords = accountUtilizationRepository.countHistoryDocument(request.orgId, request.accountType, request.startDate, request.endDate)
        var historyDocuments = mutableListOf<HistoryDocument>()
        documents?.forEach { doc -> historyDocuments.add(historyDocumentConverter.convertToModel(doc)) }
        return ResponseList(
            list = historyDocuments,
            totalPages = getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    private fun stringAccountTypes(request: SettlementHistoryRequest): MutableList<String> {
        var accountTypes = mutableListOf<String>()
        request.accountType.forEach { accountType ->
            accountTypes.add(accountType.toString())
        }
        return accountTypes
    }

    /**
     * Get Total pages from page size and total records
     * @param totalRows
     * @param pageSize
     * @return totalPages
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
     * Get Settlement details for input document number
     * @param SettlementRequest
     * @return ResponseList
     */
    override suspend fun getSettlement(request: SettlementRequest): ResponseList<com.cogoport.ares.model.settlement.SettledInvoice?> {
        var settledDocuments = mutableListOf<com.cogoport.ares.model.settlement.SettledInvoice>()
        var settlements = mutableListOf<SettledInvoice>()
        when (request.settlementType) {
            SettlementType.REC, SettlementType.PCN -> {
                settlements = settlementRepository.findSettlement(
                    request.documentNo,
                    request.settlementType,
                    request.page,
                    request.pageLimit
                ) as MutableList<SettledInvoice>
            }
        }

        var totalRecords = settlementRepository.countSettlement(request.documentNo, request.settlementType)

        settlements?.forEach {
            settlement ->
            when (request.settlementType) {
                SettlementType.REC, SettlementType.PCN -> {
                    settledDocuments.add(settledInvoiceConverter.convertToModel(settlement))
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

    /**
     * Get List of Documents from OpenSearch index_account_utilization
     * @param SettlementDocumentRequest
     * @return ResponseList
     */
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

    /**
     *
     * @param SearchResponse
     * @return List
     */
    private fun invoiceListResponses(clientResponse: SearchResponse<AccountUtilizationResponse>?): List<Document>? {
        val data = clientResponse?.hits()?.hits()?.map {
            val response = it.source()
            val tds = response?.amountCurr!! * AresConstants.TWO_PERCENT.toBigDecimal()
            val afterTdsAmount = response.amountCurr!! - tds
            val settledAmount = response.payCurr!!
            val balanceAmount = afterTdsAmount - settledAmount
            val status = getInvoiceStatus(afterTdsAmount, balanceAmount) // should come from index
            // TODO("add taxable amount to account utilization index from plutus")
            // TODO("add status column in account utilizations")
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

    override suspend fun check(request: CheckRequest): List<CheckDocument> = runSettlement(request, false)

    override suspend fun settle(request: CheckRequest): List<CheckDocument> = runSettlement(request, true)

    private suspend fun runSettlement(request: CheckRequest, performDbOperation: Boolean): List<CheckDocument> {
        sanitizeInput(request)
        val source = mutableListOf<CheckDocument>()
        val dest = mutableListOf<CheckDocument>()
        val creditType = listOf(SettlementType.REC, SettlementType.PCN, SettlementType.PAY, SettlementType.SCN)
        val debitType = listOf(SettlementType.SINV, SettlementType.PINV, SettlementType.SDN, SettlementType.PDN)
        for (doc in request.stackDetails.reversed()) {
            if (creditType.contains(doc.accountType)) { source.add(doc) } else if (debitType.contains(doc.accountType)) { dest.add(doc) }
        }
        if (source.isEmpty() && dest.map { it.accountType }.contains(SettlementType.SINV) && dest.map { it.accountType }.contains(SettlementType.PINV)) {
            dest.filter { it.accountType == SettlementType.SINV }.forEach {
                source.add(it)
                dest.remove(it)
            }
        }
        businessValidation(source, dest)
        val settledList = settleDocuments(request, source, dest, performDbOperation)
        return request.stackDetails.map { r -> settledList.filter { it.id == r.id }[0] }
    }

    private suspend fun settleDocuments(request: CheckRequest, source: MutableList<CheckDocument>, dest: MutableList<CheckDocument>, performDbOperation: Boolean): MutableList<CheckDocument> {
        val response = mutableListOf<CheckDocument>()
        for (payment in source) {
            var availableAmount = payment.allocationAmount
            val canSettle = fetchSettlingDocs(payment.accountType)
            for (invoice in dest) {
                if (canSettle.contains(invoice.accountType)) {
                    availableAmount = doSettlement(request, invoice, availableAmount, payment, source, performDbOperation)
                }
            }
            payment.allocationAmount -= availableAmount
            payment.balanceAfterAllocation = payment.balanceAmount.subtract(payment.allocationAmount)
            assignStatus(payment)
            response.add(payment)
        }
        dest.forEach { response.add(it) }
        return response
    }

    private suspend fun doSettlement(request: CheckRequest, invoice: CheckDocument, availableAmount: BigDecimal, payment: CheckDocument, source: MutableList<CheckDocument>, performDbOperation: Boolean): BigDecimal {
        var amount = availableAmount
        val toSettleAmount = invoice.allocationAmount - invoice.settledAmount
        if (toSettleAmount != 0.0.toBigDecimal()) {
            var rate = 1.toBigDecimal()
            val ledgerRate = payment.exchangeRate
            var updateDoc = true
            if (payment.currency != invoice.currency) {
                rate = if (payment.legCurrency == invoice.currency) {
                    ledgerRate
                } else {
                    getExchangeRate(payment.currency, invoice.currency, payment.transactionDate)
                }
                amount = getExchangeValue(availableAmount, rate)
            }
            if (amount >= toSettleAmount) {
                amount = updateDocuments(request, invoice, payment, toSettleAmount, amount, rate, ledgerRate, updateDoc, performDbOperation)
            } else if (amount < toSettleAmount) {
                if (payment != source.last()) updateDoc = false
                amount = updateDocuments(request, invoice, payment, amount, amount, rate, ledgerRate, updateDoc, performDbOperation)
            }
        }
        return amount
    }

    private suspend fun updateDocuments(request: CheckRequest, invoice: CheckDocument, payment: CheckDocument, toSettleAmount: BigDecimal, availableAmount: BigDecimal, exchangeRate: BigDecimal, ledgerRate: BigDecimal, updateDoc: Boolean, performDbOperation: Boolean): BigDecimal {
        val invoiceTds = invoice.tds!! - invoice.settledTds
        var paymentTds = getExchangeValue(invoiceTds, exchangeRate, true)
        if (payment.accountType !in (listOf(SettlementType.PCN, SettlementType.SCN))) {
            paymentTds = 0.toBigDecimal()
        }
        val amount = availableAmount - toSettleAmount - paymentTds
        invoice.settledAmount += toSettleAmount
        payment.settledAmount += getExchangeValue(toSettleAmount + paymentTds, exchangeRate, true)
        if (updateDoc) {
            invoice.allocationAmount = invoice.settledAmount
            invoice.balanceAfterAllocation = invoice.balanceAmount.subtract(invoice.allocationAmount)
        }
        assignStatus(invoice)
        assignStatus(payment)
        if (performDbOperation) performDbOperation(request, toSettleAmount, exchangeRate, ledgerRate, payment, invoice)
        return getExchangeValue(amount, exchangeRate, true)
    }

    private suspend fun performDbOperation(request: CheckRequest, toSettleAmount: BigDecimal, exchangeRate: BigDecimal, ledgerRate: BigDecimal, payment: CheckDocument, invoice: CheckDocument) {
        val paidAmount = getExchangeValue(toSettleAmount, exchangeRate, true)
        val paidLedAmount = getExchangeValue(paidAmount, ledgerRate)
        val invoiceTds = invoice.tds!! - invoice.settledTds
        val paymentTds = getExchangeValue(invoiceTds, exchangeRate, true)
        val paymentTdsLed = getExchangeValue(paymentTds, ledgerRate)
        val doc = createSettlement(payment.documentNo, payment.accountType, invoice, payment.currency, (paidAmount + paymentTds), payment.legCurrency, (paidLedAmount + paymentTdsLed), 1, request.settlementDate)
        if (paymentTds.compareTo(0.toBigDecimal()) != 0) {
            val tdsType = if (fetchSettlingDocs(SettlementType.CTDS).contains(invoice.accountType)) SettlementType.CTDS else SettlementType.VTDS
            val tdsDoc = createSettlement(payment.documentNo, tdsType, invoice, payment.currency, paymentTds, invoice.legCurrency, paymentTdsLed, -1, request.settlementDate)
            invoice.settledTds += invoiceTds
        }
        if (payment.legCurrency != invoice.currency) {
            val excLedAmount = getExchangeValue(toSettleAmount, invoice.exchangeRate) - (paidLedAmount)
            val exType = if (fetchSettlingDocs(SettlementType.CTDS).contains(invoice.accountType)) SettlementType.SECH else SettlementType.PECH
            val exSign = excLedAmount.signum() * if (payment.accountType in listOf(SettlementType.SCN, SettlementType.REC, SettlementType.SINV)) -1 else 1
            val excDoc = createSettlement(payment.documentNo, exType, invoice, null, null, invoice.legCurrency, excLedAmount.abs(), exSign.toShort(), request.settlementDate)
        }
        val paymentUtilized = paidAmount + if (payment.accountType in listOf(SettlementType.PCN, SettlementType.SCN)) paymentTds else 0.toBigDecimal()
        updateAccountUtilization(payment, paymentUtilized)
        updateAccountUtilization(invoice, (toSettleAmount + invoiceTds))
    }

    private suspend fun updateAccountUtilization(document: CheckDocument, utilizedAmount: BigDecimal) {
        val paymentUtilization = accountUtilizationRepository.findRecord(document.documentNo, document.accountType.toString()) ?: throw AresException(AresError.ERR_1503, "${document.documentNo}_${document.accountType}")
        paymentUtilization.payCurr += utilizedAmount
        paymentUtilization.payLoc += getExchangeValue(utilizedAmount, document.exchangeRate)
        accountUtilizationRepository.update(paymentUtilization)
    }

    private suspend fun createSettlement(sourceId: Long?, sourceType: SettlementType, invoice: CheckDocument, currency: String?, amount: BigDecimal?, ledCurrency: String, ledAmount: BigDecimal, signFlag: Short, transactionDate: Timestamp) {
        val settledDoc = Settlement(
            null,
            sourceId,
            sourceType,
            invoice.documentNo,
            invoice.accountType,
            currency,
            amount,
            ledCurrency,
            ledAmount,
            signFlag,
            transactionDate,
            null,
            Timestamp.from(Instant.now()),
            null,
            Timestamp.from(Instant.now())
        )
        settlementRepository.save(settledDoc)
    }

    private fun getExchangeValue(amount: BigDecimal, exchangeRate: BigDecimal, reverse: Boolean = false): BigDecimal {
        return if (reverse) {
            Utilities.binaryOperation(amount, exchangeRate, Operator.DIVIDE)
        } else {
            Utilities.binaryOperation(amount, exchangeRate, Operator.MULTIPLY)
        }
    }

    private fun getExchangeRate(from: String, to: String, transactionDate: Timestamp): BigDecimal {
        return if (from == "USD" && to == "INR") {
            70.toBigDecimal()
        } else if (from == "INR" && to == "USD") {
            0.0142857142857.toBigDecimal()
        } else if (from == "USD" && to == "EUR") {
            0.5.toBigDecimal()
        } else if (from == "EUR" && to == "USD") {
            2.toBigDecimal()
        } else {
            1.toBigDecimal()
        }
    }
    private fun fetchSettlingDocs(accType: SettlementType): List<SettlementType> {
        return when (accType) {
            SettlementType.REC -> { listOf(SettlementType.SINV, SettlementType.SDN) }
            SettlementType.PINV -> { listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV) }
            SettlementType.PCN -> { listOf(SettlementType.PINV, SettlementType.PDN) }
            SettlementType.PAY -> { listOf(SettlementType.PINV, SettlementType.PDN) }
            SettlementType.SINV -> { listOf(SettlementType.REC, SettlementType.SCN, SettlementType.PINV) }
            SettlementType.SCN -> { listOf(SettlementType.SINV, SettlementType.SDN) }
            SettlementType.SDN -> { listOf(SettlementType.SCN, SettlementType.REC) }
            SettlementType.PDN -> { listOf(SettlementType.PCN, SettlementType.PAY) }
            SettlementType.CTDS -> { listOf(SettlementType.SINV, SettlementType.SDN) }
            SettlementType.VTDS -> { listOf(SettlementType.PINV, SettlementType.PDN) }
            else -> { emptyList() }
        }
    }

    private fun sanitizeInput(request: CheckRequest) {
        for (doc in request.stackDetails) {
            if (doc.documentNo == 0.toLong()) throw AresException(AresError.ERR_1003, "Document Number")
        }
    }

    private fun businessValidation(source: MutableList<CheckDocument>, dest: MutableList<CheckDocument>) {
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
        if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAmount)) == 0) {
            doc.documentStatus = InvoiceStatus.KNOCKED_OFF
        } else if (decimalRound(doc.settledAmount).compareTo(0.toBigDecimal()) == 0) {
            doc.documentStatus = InvoiceStatus.UNPAID
        } else if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAmount)) == 1) {
            doc.documentStatus = InvoiceStatus.PARTIAL_PAID
        }
    }

    private fun decimalRound(amount: BigDecimal): BigDecimal {
        return Utilities.decimalRound(amount)
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
            AccountType.SCN -> { InvoiceType.SALES_CREDIT_NOTE }
            AccountType.SDN -> { InvoiceType.SALES_DEBIT_NOTE }
            AccountType.REC -> { InvoiceType.SALES_PAYMENT }
            AccountType.PINV -> { InvoiceType.PURCHASE_INVOICES }
            AccountType.PCN -> { InvoiceType.PURCHASE_CREDIT_NOTE }
            AccountType.PDN -> { InvoiceType.PURCHASE_DEBIT_NOTE }
            AccountType.PAY -> { InvoiceType.PURCHASE_PAYMENT }
        }
    }
}
