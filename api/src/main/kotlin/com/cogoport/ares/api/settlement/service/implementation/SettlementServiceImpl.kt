package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
import com.cogoport.ares.api.settlement.mapper.SettledInvoiceMapper
import com.cogoport.ares.api.settlement.mapper.SettlementMapper
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.model.payment.*
import com.cogoport.ares.model.settlement.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.opensearch.client.opensearch.core.SearchResponse
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

@Singleton
class SettlementServiceImpl : SettlementService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var historyDocumentConverter: HistoryDocumentMapper

    @Inject
    lateinit var settlementConvert: SettlementMapper

    @Inject
    lateinit var settledInvoiceConverter: SettledInvoiceMapper

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    override suspend fun getDocuments(request: SettlementDocumentRequest) = getDocumentsFromOpenSearch(request)

    /***
    - add entry into payments table
	- add into account utilizations
	- add entries into settlement table
		- invoice knocked off with amount
		- tds entry
		- payment entry
		- convinence fee entry [like EXC/TDS] for accounting of payment
	- update utilization table with balance or status
     */
    override suspend fun knockoff(request: SettlementKnockoffRequest): SettlementKnockoffResponse {

        val exchangeRate = BigDecimal.ONE // TODO Get Exchange Rate for TODAY

        // TODO DocumentValue
        val invoiceUtilization = accountUtilizationRepository.findRecord(documentNo = request.invoiceNumber, accType = "SINV")

        if(invoiceUtilization == null){
            throw AresException(AresError.ERR_1002, AresConstants.ZONE)
        }

//        val payment = settledInvoiceConverter.convertKnockoffRequestToEntity(request)
//        payment.organizationId = invoiceUtilization?.organizationId
//        payment.organizationName = invoiceUtilization?.organizationName
//        payment.exchangeRate = exchangeRate

//        Utilization of payment
        val documentNo = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.RECEIVED.prefix)
        val accountUtilization = AccountUtilization(
            id = null,
            documentNo = documentNo,
            documentValue = request.transactionId,
            zoneCode = invoiceUtilization.zoneCode,
            serviceType = invoiceUtilization.serviceType,
            documentStatus =  DocumentStatus.FINAL,
            entityCode = invoiceUtilization.entityCode,
            category = invoiceUtilization.category,
            orgSerialId = invoiceUtilization.orgSerialId,
            sageOrganizationId = invoiceUtilization.sageOrganizationId,
            organizationId = invoiceUtilization.organizationId,
            organizationName = invoiceUtilization.organizationName,
            accType = invoiceUtilization.accType,
            accCode = invoiceUtilization.accCode,
            signFlag = 1,
            currency = request.currency,
            ledCurrency = invoiceUtilization.ledCurrency,
            amountCurr = request.amount,
            amountLoc = request.amount,
            taxableAmount = BigDecimal.ZERO,
            payCurr = BigDecimal.ZERO,
            payLoc = BigDecimal.ZERO,
            accMode = invoiceUtilization.accMode,
            transactionDate = Date(request.transactionDate),
            dueDate = Date(request.transactionDate)
        )

        val isTdsApplied = settlementRepository.countDestinationBySourceType(invoiceUtilization.documentNo, SettlementType.SINV, SettlementType.CTDS) > 0

        var settlements = mutableListOf<Settlement>()
//        settlements.add(
//            Settlement(
//                id = null,
//
//            )
//        )
        if(isTdsApplied){
            val tds: BigDecimal = invoiceUtilization.taxableAmount.multiply(0.02.toBigDecimal())
//            settlements.add(
//                Settlement(
//
//                )
//            )

        }

//        val settlement = Settlement()


        accountUtilizationRepository.update(invoiceUtilization)
        val paymentUtilization = accountUtilizationRepository.save(accountUtilization)






//     2%   tds on taxable amount only if tds is not deducted already


        return SettlementKnockoffResponse()
    }

    private suspend fun createSettlement(sourceId: Long?, sourceType: SettlementType, invoiceId:Long,settlementType: SettlementType, currency: String?, amount: BigDecimal?, ledCurrency: String, ledAmount: BigDecimal, signFlag: Short, transactionDate: Timestamp) {
        val settledDoc = Settlement(
            null,
            sourceId,
            sourceType,
            invoiceId,
            settlementType,
            currency,
            amount,
            ledCurrency,
            ledAmount,
            signFlag,
            transactionDate
        )
        settlementRepository.save(settledDoc)
    }

    /**
     * Get invoices for Given CP orgId
     * @param SettlementDocumentRequest
     * @return ResponseList
     */
    override suspend fun getInvoices(request: SettlementDocumentRequest): ResponseList<Invoice> {
        val documents = getDocumentsFromOpenSearch(request)
        val invoices = mutableListOf<Invoice>()
        documents.list?.forEach {
            document ->
            run {
                invoices.add(
                    Invoice(
                        id = document.id,
                        invoiceNo = document.documentNo,
                        invoiceDate = document.invoiceDate,
                        dueDate = document.dueDate,
                        invoiceAmount = document.invoiceAmount,
                        taxableAmount = document.taxableAmount,
                        tds = document.tds,
                        afterTdsAmount = document.afterTdsAmount,
                        settledAmount = document.settledAmount,
                        balanceAmount = document.balanceAmount,
                        invoiceStatus = document.invoiceStatus,
                    )
                )
            }
        }
        return ResponseList(
            list = invoices,
            totalPages = documents.totalPages,
            totalRecords = documents.totalRecords,
            pageNo = documents.pageNo
        )
    }

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
    private fun getDocumentsFromOpenSearch(request: SettlementDocumentRequest): ResponseList<Document> {
        val clientResponse = OpenSearchClient().getSettlementDocuments(request)
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
     * Get List of Documents from OpenSearch index_account_utilization
     * @param SettlementDocumentRequest
     * @return ResponseList
     */
    private fun getInvoicesFromOpenSearch(request: SettlementDocumentRequest): ResponseList<Document> {
        val clientResponse = OpenSearchClient().getSettlementDocuments(request)
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
            AccountType.SCN -> { InvoiceType.SALES_CREDIT_NOTE }
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
