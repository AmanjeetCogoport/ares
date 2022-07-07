package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
import com.cogoport.ares.api.settlement.mapper.SettlementMapper
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.AccountUtilizationResponse
import com.cogoport.ares.model.payment.InvoiceStatus
import com.cogoport.ares.model.payment.InvoiceType
import com.cogoport.ares.model.settlement.SettlementDocumentRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.SettledDocument
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import io.micronaut.data.model.Pageable
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
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
        val documents = accountUtilizationRepository.getHistoryDocument(request.orgId, request.page, request.pageLimit)
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

        var totalRecords = settlementRepository.countSettlement(request.documentNo)

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
