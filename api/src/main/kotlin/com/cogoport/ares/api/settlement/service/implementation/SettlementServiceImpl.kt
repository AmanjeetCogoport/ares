package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.enums.IncidentStatus
import com.cogoport.ares.api.common.models.ListOrgStylesRequest
import com.cogoport.ares.api.common.models.TdsDataResponse
import com.cogoport.ares.api.common.models.TdsStylesResponse
import com.cogoport.ares.api.events.AresKafkaEmitter
import com.cogoport.ares.api.events.OpenSearchEvent
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.OpenSearchClient
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.PaymentData
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.IncidentMappings
import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
import com.cogoport.ares.api.settlement.mapper.OrgSummaryMapper
import com.cogoport.ares.api.settlement.mapper.SettledInvoiceMapper
import com.cogoport.ares.api.settlement.model.AccTypeMode
import com.cogoport.ares.api.settlement.model.Sid
import com.cogoport.ares.api.settlement.repository.IncidentMappingsRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import com.cogoport.ares.model.payment.Operator
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.request.DeleteSettlementRequest
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckResponse
import com.cogoport.ares.model.settlement.CreateIncidentRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.EditTdsRequest
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.OrgSummaryResponse
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import com.cogoport.ares.model.settlement.TdsSettlementDocumentRequest
import com.cogoport.ares.model.settlement.TdsStyle
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.event.InvoiceBalance
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import com.cogoport.ares.model.settlement.request.CheckRequest
import com.cogoport.ares.model.settlement.request.OrgSummaryRequest
import com.cogoport.ares.model.settlement.request.RejectSettleApproval
import com.cogoport.ares.model.settlement.request.SettlementDocumentRequest
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.hades.client.HadesClient
import com.cogoport.hades.model.incident.IncidentData
import com.cogoport.hades.model.incident.Organization
import com.cogoport.hades.model.incident.enums.IncidentType
import com.cogoport.hades.model.incident.request.UpdateIncidentRequest
import com.cogoport.kuber.client.KuberClient
import com.cogoport.kuber.model.bills.ListBillRequest
import com.cogoport.kuber.model.bills.request.UpdatePaymentStatusRequest
import com.cogoport.plutus.client.PlutusClient
import io.micronaut.context.annotation.Value
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.Date
import java.util.UUID
import javax.transaction.Transactional
import kotlin.math.ceil

@Singleton
open class SettlementServiceImpl : SettlementService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var historyDocumentConverter: HistoryDocumentMapper

    @Inject
    lateinit var settledInvoiceConverter: SettledInvoiceMapper

    @Inject
    lateinit var documentConverter: DocumentMapper

    @Inject
    lateinit var orgSummaryConverter: OrgSummaryMapper

    @Inject
    lateinit var cogoClient: AuthClient

    @Inject
    lateinit var plutusClient: PlutusClient

    @Inject
    lateinit var aresKafkaEmitter: AresKafkaEmitter

    @Inject
    lateinit var settlementServiceHelper: SettlementServiceHelper

    @Inject
    lateinit var auditService: AuditService

    @Inject
    lateinit var hadesClient: HadesClient

    @Inject
    lateinit var incidentMappingsRepository: IncidentMappingsRepository

    @Inject
    lateinit var kuberClient: KuberClient

    @Value("\${ares.settlement.crossTradeParty:false}")
    private var crossTradeParty: Boolean = false

    @Inject
    private lateinit var journalVoucherService: JournalVoucherService

    /**
     * Get documents for Given Business partner/partners in input request.
     * @param settlementDocumentRequest
     * @return ResponseList
     */
    override suspend fun getDocuments(settlementDocumentRequest: SettlementDocumentRequest): ResponseList<Document>? {
        validateSettlementDocumentInput(settlementDocumentRequest)
        return getDocumentList(settlementDocumentRequest)
    }

    /**
     */
    override suspend fun getTDSDocuments(
        request: TdsSettlementDocumentRequest
    ): ResponseList<Document> {
        validateTdsDocumentInput(request)
        return getTDSDocumentList(request)
    }

    /**
     * Get Account balance of selected Business Partners.
     * @param summaryRequest
     * @return SummaryResponse
     */
    override suspend fun getAccountBalance(summaryRequest: SummaryRequest): SummaryResponse {
        val orgId = getOrgIds(summaryRequest.importerExporterId, summaryRequest.serviceProviderId)
        val accTypeMode = getAccountModeAndType(summaryRequest.importerExporterId, summaryRequest.serviceProviderId, null)
        val accType = accTypeMode.accType
        val accMode = accTypeMode.accMode
        val amount =
            accountUtilizationRepository.getAccountBalance(
                orgId,
                summaryRequest.entityCode!!,
                summaryRequest.startDate,
                summaryRequest.endDate,
                accType,
                accMode
            )
        val onAccountPayment =
            accountUtilizationRepository.getAccountBalance(
                orgId,
                summaryRequest.entityCode!!,
                null,
                null,
                listOf(AccountType.REC, AccountType.PAY),
                accMode
            )
        return SummaryResponse(
            amount = amount,
            onAccountAmount = onAccountPayment
        )
    }

    /**
     * Get History Document list (Credit Notes and On Account Payments).
     * @param request
     * @return ResponseList<HistoryDocument>
     */
    override suspend fun getHistory(
        request: SettlementHistoryRequest
    ): ResponseList<HistoryDocument?> {
        val accountTypes = stringAccountTypes(request)

        var paymentIds: List<Long> = emptyList()
        if (request.query != "") {
            paymentIds = settlementRepository.getPaymentIds(query = request.query)
        }
        val documents =
            accountUtilizationRepository.getHistoryDocument(
                request.orgId!!,
                accountTypes,
                request.page,
                request.pageLimit,
                request.startDate,
                request.endDate,
                request.query,
                paymentIds,
                request.sortBy,
                request.sortType
            )

        val totalRecords =
            accountUtilizationRepository.countHistoryDocument(
                request.orgId!!,
                accountTypes,
                request.startDate,
                request.endDate,
                request.query,
                paymentIds
            )

        val historyDocuments = mutableListOf<HistoryDocument>()
        documents.forEach { doc ->
            historyDocuments.add(historyDocumentConverter.convertToModel(doc))
        }
        historyDocuments.forEach {
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.id = it.id?.let { it1 -> Hashids.encode(it1.toLong()) }
        }
        return ResponseList(
            list = historyDocuments,
            totalPages = Utilities.getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    private fun stringAccountTypes(request: SettlementHistoryRequest): MutableList<String> {
        val accountTypes =
            if (request.accountType == AresConstants.ALL) {
                mutableListOf(
                    AccountType.PCN.toString(), AccountType.REC.toString(), AccountType.PAY.toString(),
                    AccountType.SINV.toString(), AccountType.SCN.toString()
                )
            } else if (request.accountType == "REC") {
                mutableListOf(AccountType.REC.toString(), AccountType.PAY.toString())
            } else if (request.accountType == "SINV") {
                mutableListOf(AccountType.SINV.toString())
            } else {
                mutableListOf(request.accountType!!)
            }
        return accountTypes
    }

    /**
     * Get Settlement details for input document number
     * @param request
     * @return ResponseList
     */
    override suspend fun getSettlement(
        request: SettlementRequest
    ): ResponseList<com.cogoport.ares.model.settlement.SettledInvoice?> {
        request.documentNo = Hashids.decode(request.documentNo)[0].toString()
        val settlementGrouped = getSettlementFromDB(request)
        val paymentIds = mutableListOf(request.documentNo.toLong())
        val payments = getPaymentDataForSettledInvoices(settlementGrouped, paymentIds, request.settlementType)
        val settlements = getSettledInvoices(settlementGrouped, payments, request.documentNo.toLong())
        // Fetch Sid for invoices
        val docIds = settlements.map { it.destinationId.toString() }
        val sids = getSidsForInvoices(docIds, request.settlementType)
        val settledDocuments = populateSettledDocuments(settlements, request, payments, sids)

        // Pagination Data
        val totalRecords =
            settlementRepository.countSettlement(request.documentNo.toLong(), request.settlementType)
        settledDocuments.forEach {
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.id = it.id?.let { it1 -> Hashids.encode(it1.toLong()) }
        }
        return ResponseList(
            list = settledDocuments,
            totalPages = Utilities.getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    private suspend fun getSidsForInvoices(ids: List<String>, accType: SettlementType): List<Sid>? {
        return if (ids.isNotEmpty()) {
            try {
                if (accType == SettlementType.REC) {
                    plutusClient.getSidsForInvoiceIds(ids)?.map {
                        Sid(
                            documentId = it.invoiceId,
                            jobNumber = it.jobNumber
                        )
                    }
                } else if (accType in listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV)) {
                    kuberClient.getSidsForBillIds(ids).map {
                        Sid(
                            documentId = it.billId,
                            jobNumber = it.jobNumber
                        )
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                logger().error(e.stackTraceToString())
                null
            }
        } else null
    }

    private suspend fun getSettledInvoices(
        settlementGrouped: Map<Long?, List<SettledInvoice>>,
        payments: List<PaymentData>,
        paymentRequested: Long
    ): MutableList<SettledInvoice> {
        val settlements = mutableListOf<SettledInvoice>()
        for (docList in settlementGrouped) {
            val settledTds = docList.value.sumOf { doc ->
                if (!doc.tdsCurrency.isNullOrBlank()) {
                    val rate = payments.find { it.documentNo == doc.tdsDocumentNo }?.exchangeRate
                    val date = payments.find { it.documentNo == doc.tdsDocumentNo }?.transactionDate
                        ?: throw AresException(AresError.ERR_1503, "${doc.tdsDocumentNo}")
                    convertPaymentCurrToInvoiceCurr(
                        toCurrency = doc.currency!!,
                        fromCurrency = doc.tdsCurrency,
                        fromLedCurrency = doc.ledCurrency,
                        amount = doc.settledTds,
                        exchangeRate = rate,
                        exchangeDate = date
                    )
                } else {
                    BigDecimal.ZERO
                }
            }
            docList.value.map {
                it.settledTds = settledTds
            }
            val settledInvoice = docList.value.find { it.tdsDocumentNo == paymentRequested } ?: docList.value.first()
            settlements.add(settledInvoice)
        }
        return settlements
    }

    private suspend fun populateSettledDocuments(
        settlements: MutableList<SettledInvoice>,
        request: SettlementRequest,
        payments: List<PaymentData>,
        sids: List<Sid>?
    ): MutableList<com.cogoport.ares.model.settlement.SettledInvoice> {
        val settledDocuments = mutableListOf<com.cogoport.ares.model.settlement.SettledInvoice>()
        settlements.forEach { settlement ->
            when (request.settlementType) {
                SettlementType.REC, SettlementType.PCN, SettlementType.PAY, SettlementType.SINV, SettlementType.SCN -> {
                    // Calculate Settled Amount in Invoice Currency
                    settlement.settledAmount =
                        getAmountInInvoiceCurrency(settlement, payments, settlement.settledAmount)
                    // Calculate Tds in Invoice Currency
                    settlement.tds = getAmountInInvoiceCurrency(settlement, payments, settlement.tds)
                    // Calculate Nostro in Invoice Currency
                    settlement.nostroAmount =
                        getAmountInInvoiceCurrency(settlement, payments, settlement.nostroAmount)

                    // Convert To Model
                    val settledDoc = settledInvoiceConverter.convertToModel(settlement)
                    settledDoc.settledAmount -= (settledDoc.tds + settledDoc.nostroAmount)
                    settledDoc.balanceAmount = settledDoc.currentBalance
                    settledDoc.allocationAmount = settledDoc.settledAmount
                    settledDoc.afterTdsAmount -= settledDoc.settledTds
                    settledDoc.currentBalance += (settledDoc.tds + settledDoc.nostroAmount)
                    settledDoc.settledTds -= settledDoc.tds

                    // Assign Sid
                    settledDoc.sid = sids?.find { it.documentId == settledDoc.documentNo.toLong() }?.jobNumber
                    // Assign Status
                    val paid = (settledDoc.documentAmount - (settledDoc.settledAmount + settledDoc.tds + settledDoc.nostroAmount))
                    if (paid.compareTo(BigDecimal.ZERO) == 0) {
                        settledDoc.status = DocStatus.KNOCKED_OFF.value
                    } else if (paid.compareTo(settledDoc.documentAmount) == 0) {
                        settledDoc.status = DocStatus.UNPAID.value
                    } else {
                        settledDoc.status = DocStatus.PARTIAL_PAID.value
                    }
                    settledDocuments.add(settledDoc)
                }
                else -> {}
            }
        }
        return settledDocuments
    }

    private suspend fun getAmountInInvoiceCurrency(
        settlement: SettledInvoice,
        payments: List<PaymentData>,
        amountPaymentCurr: BigDecimal
    ) = convertPaymentCurrToInvoiceCurr(
        toCurrency = settlement.currency!!,
        fromCurrency = settlement.paymentCurrency,
        fromLedCurrency = settlement.ledCurrency,
        amount = amountPaymentCurr,
        exchangeRate = payments.find { it.documentNo == settlement.paymentDocumentNo }?.exchangeRate,
        exchangeDate = payments.find { it.documentNo == settlement.paymentDocumentNo }?.transactionDate!!
    )

    /**
     * Get Payment date, exchange rate for the settled TDS.
     * @param: settlementGrouped
     * @param: paymentIds
     * @param: settlementType
     */
    private suspend fun getPaymentDataForSettledInvoices(
        settlementGrouped: Map<Long?, List<SettledInvoice>>,
        paymentIds: MutableList<Long>,
        settlementType: SettlementType
    ): List<PaymentData> {
        val tdsType = mutableListOf(settlementType)
        settlementGrouped.forEach { docList ->
            docList.value.forEach {
                if (it.tdsDocumentNo != null)
                    paymentIds.add(it.tdsDocumentNo)
                it.tdsType?.let { it1 ->
                    when (it1) {
                        SettlementType.CTDS -> tdsType.addAll(
                            listOf(SettlementType.REC, SettlementType.SCN, SettlementType.SINV)
                        )
                        SettlementType.VTDS -> tdsType.addAll(
                            listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV)
                        )
                        else -> tdsType.add(it1)
                    }
                }
            }
        }

        val payments = accountUtilizationRepository.getPaymentDetails(paymentIds.distinct(), tdsType.distinct())
        payments.forEach {
            if (it.documentNo == null) throw AresException(AresError.ERR_1503, "")
            if (it.transactionDate == null) throw AresException(AresError.ERR_1005, "transactionDate")
        }
        return payments
    }

    /**
     * Get settled document from DB for the payment in request.
     * @param: request
     * @return: Map<Long?, List<SettledInvoice>>
     */
    private suspend fun getSettlementFromDB(request: SettlementRequest): Map<Long?, List<SettledInvoice>> {
        var settlements = mutableListOf<SettledInvoice>()
        when (request.settlementType) {
            SettlementType.REC, SettlementType.PCN, SettlementType.PAY, SettlementType.SINV, SettlementType.SCN -> {
                @Suppress("UNCHECKED_CAST")
                settlements =
                    settlementRepository.findSettlement(
                    request.documentNo.toLong(),
                    request.settlementType,
                    request.page,
                    request.pageLimit
                ) as MutableList<SettledInvoice>
            }
            else -> {}
        }
        // Group Invoices And Calculate settled Tds
        return settlements.groupBy { it.id }
    }

    private suspend fun convertPaymentCurrToInvoiceCurr(
        toCurrency: String,
        fromCurrency: String,
        fromLedCurrency: String,
        exchangeRate: BigDecimal?,
        amount: BigDecimal,
        exchangeDate: Date
    ): BigDecimal {
        return if (toCurrency != fromCurrency) {
            val rate = if (fromLedCurrency == toCurrency) {
                exchangeRate
                    ?: settlementServiceHelper.getExchangeRate(
                        fromCurrency, toCurrency,
                        SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(exchangeDate)
                    )
            } else {
                settlementServiceHelper.getExchangeRate(
                    fromCurrency, toCurrency,
                    SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(exchangeDate)
                )
            }

            Utilities.binaryOperation(
                operandOne = amount,
                operandTwo = rate,
                operation = Operator.MULTIPLY
            )
        } else {
            amount
        }
    }

    private fun calculateTds(rate: BigDecimal, settledTds: BigDecimal, taxableAmount: BigDecimal): BigDecimal {
        val tds =
            taxableAmount * Utilities.binaryOperation(
                rate, 100.toBigDecimal(), Operator.DIVIDE
            ).setScale(AresConstants.ROUND_DECIMAL_TO, RoundingMode.HALF_DOWN)
        return if (tds >= settledTds) {
            tds - settledTds
        } else if (settledTds.compareTo(BigDecimal.ZERO) == 0) {
            tds
        } else BigDecimal.ZERO
    }

    /**
     * Get List of Documents from OpenSearch index_account_utilization
     * @param request
     * @return ResponseList
     */
    private suspend fun getDocumentList(
        request: SettlementDocumentRequest
    ): ResponseList<Document> {
        val offset = (request.pageLimit * request.page) - request.pageLimit
        val orgId = getOrgIds(request.importerExporterId, request.serviceProviderId)
        val accTypeMode = getAccountModeAndType(request.importerExporterId, request.serviceProviderId, request.docType)
        val accType = accTypeMode.accType
        val accMode = accTypeMode.accMode
        val isTransactionDateSortTypeDesc = request.isTransactionDateSortTypeDesc
        val documentEntity =
            accountUtilizationRepository.getDocumentList(
                request.pageLimit,
                offset,
                accType,
                orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                "${request.query}%",
                accMode,
                isTransactionDateSortTypeDesc
            )
        if (documentEntity.isEmpty()) return ResponseList()

        val tradePartyMappingIds = documentEntity
            .filter { document -> document!!.mappingId != null }
            .map { document -> document!!.mappingId.toString() }
            .distinct()
        val documentModel = groupDocumentList(documentEntity).map { documentConverter.convertToModel(it!!) }
        documentModel.forEach {
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.id = Hashids.encode(it.id.toLong())
        }
        val tdsProfiles = listOrgTdsProfile(tradePartyMappingIds)
        val total =
            accountUtilizationRepository.getDocumentCount(
                accType,
                orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                "${request.query}%"
            )
        for (doc in documentModel) {
            val tdsElement = tdsProfiles.find { it.id == doc.mappingId }
            val rate = getTdsRate(tdsElement)
            doc.tds = calculateTds(
                rate = rate,
                settledTds = doc.settledTds!!,
                taxableAmount = doc.taxableAmount
            )
            doc.afterTdsAmount -= (doc.tds + doc.settledTds!!)
            doc.balanceAmount -= doc.tds
            doc.documentType = settlementServiceHelper.getDocumentType(AccountType.valueOf(doc.documentType), doc.signFlag, doc.accMode)
            doc.status = settlementServiceHelper.getDocumentStatus(
                docAmount = doc.documentAmount,
                balanceAmount = doc.currentBalance,
                docType = SettlementType.valueOf(doc.accountType)
            )
            doc.settledAllocation = BigDecimal.ZERO
            doc.allocationAmount = doc.balanceAmount
            doc.balanceAfterAllocation = BigDecimal.ZERO
        }

        val billListIds = documentModel.filter { it.accountType in listOf("PINV", "SREIMB") }.map { it.documentNo }

        val listBillRequest = ListBillRequest(
            jobNumbers = null,
            jobType = null,
            status = null,
            excludeStatus = null,
            organizationId = null,
            serviceProviderOrgId = null,
            paymentStatus = null,
            serviceType = null,
            billNumber = null,
            urgencyTag = null,
            from = null,
            to = null,
            q = null,
            billType = null,
            proforma = null,
            serviceOpIds = null,
            billIds = billListIds
        )

        val responseList = kuberClient.billListByIds(listBillRequest)

        responseList.list?.map { it ->
            val documentId = it.billId
            if (documentModel.any { k -> k.documentNo == documentId }) {
                documentModel.first { k -> k.documentNo == documentId }.hasPayrun = it.hasPayrun!!
            }
        }

        return ResponseList(
            list = documentModel,
            totalPages = ceil(total?.toDouble()?.div(request.pageLimit) ?: 0.0).toLong(),
            totalRecords = total,
            pageNo = request.page
        )
    }

    /**
     * Get TDS Deduction styles for list of trade party mapping id.
     * @param: tradePartyMappingIds
     * @return: List
     */
    private suspend fun listOrgTdsProfile(tradePartyMappingIds: List<String>): List<TdsStylesResponse> {
        var tdsStylesResponse = mutableListOf<TdsStylesResponse>()
        var tdsStylesFromClient: List<TdsDataResponse>? = null
        try {
            tdsStylesFromClient = cogoClient.listOrgTdsStyles(request = ListOrgStylesRequest(ids = tradePartyMappingIds))
        } catch (_: Exception) {
            null
        }
        tdsStylesResponse = assignClientResponse(tdsStylesResponse, tradePartyMappingIds, tdsStylesFromClient)

        return tdsStylesResponse
    }

    /**
     * Assign client response TDS Styles to all the mapping Ids.
     * @param: tdsStylesResponse
     * @param: tradePartyMappingIds
     */
    private fun assignClientResponse(
        tdsStylesResponse: MutableList<TdsStylesResponse>,
        tradePartyMappingIds: List<String>,
        tdsStylesFromClient: List<TdsDataResponse>?
    ): MutableList<TdsStylesResponse> {
        for (tradePartyMapping in tradePartyMappingIds) {
            val tdsElement = tdsStylesFromClient?.find { it.data.id.toString() == tradePartyMapping }?.data
            if (tdsElement != null) {
                tdsStylesResponse.add(tdsElement)
            } else {
                addDefaultStyle(tdsStylesResponse, tradePartyMapping)
            }
        }
        return tdsStylesResponse
    }

    /**
     * Assign and add Default profile to input mapping Id.
     * @param: tdsStylesResponse
     * @param: tradePartyMappingIds
     */
    private fun addDefaultStyle(
        tdsStylesResponse: MutableList<TdsStylesResponse>,
        tradePartyMapping: String
    ) {
        tdsStylesResponse.add(
            TdsStylesResponse(
                id = UUID.fromString(tradePartyMapping),
                tdsDeductionStyle = "gross",
                tdsDeductionType = "normal",
                tdsDeductionRate = AresConstants.DEFAULT_TDS_RATE.toBigDecimal()
            )
        )
    }

    /**
     * Get TDS Deduction styles for trade party detail's self mapping id.
     * @param: tradePartyDetailsId
     * @return: TdsStylesResponse
     */
    private suspend fun getSelfOrgTdsProfile(tradePartyDetailsId: UUID): TdsStylesResponse {
        var tdsStylesResponse = TdsStylesResponse(
            id = tradePartyDetailsId,
            tdsDeductionStyle = "gross",
            tdsDeductionType = "no_deductions",
            tdsDeductionRate = 2.toBigDecimal()
        )
        try {
            tdsStylesResponse = cogoClient.getSelfOrgTdsStyles(tradePartyDetailsId.toString()).data
        } catch (_: Exception) {
            null
        }
        return tdsStylesResponse
    }

    /**
     * Groups document in case of.
     * @param: documentEntity
     * @return: List
     */
    private suspend fun groupDocumentList(documentEntity: List<com.cogoport.ares.api.settlement.entity.Document?>): List<com.cogoport.ares.api.settlement.entity.Document?> {
        return documentEntity.groupBy { it!!.id }.map { docList ->
            val settledTds = docList.value.sumOf { doc ->
                if (doc != null) {
                    calculateSettledTds(doc)
                } else {
                    BigDecimal.ZERO
                }
            }
            docList.value.map { it?.settledTds = settledTds }
            docList.value.first()
        }
    }

    /**
     * Get organization id list from importerExporterId and serviceProviderId.
     * @param: importerExporterId
     * @param: serviceProviderId
     * @return: List
     */
    private fun getOrgIds(importerExporterId: UUID?, serviceProviderId: UUID?): List<UUID> {
        val orgId = mutableListOf<UUID>()
        if (importerExporterId != null)
            orgId.add(importerExporterId)
        if (serviceProviderId != null)
            orgId.add(serviceProviderId)
        return orgId
    }

    /**
     * Get Account Mode and Account Type based on importerExportId and serviceProviderId in the request.
     * @param: importerExporterId
     * @param: serviceProviderId
     * @return: AccTypeMode
     */
    private fun getAccountModeAndType(importerExporterId: UUID?, serviceProviderId: UUID?, docType: String?): AccTypeMode {
        val accTypeList: List<AccountType>
        return if (importerExporterId != null && serviceProviderId != null) {
            accTypeList = getListOfAccountTypeFromDocType(docType, null)
            AccTypeMode(accMode = null, accType = accTypeList)
        } else if (importerExporterId != null) {
            accTypeList = getListOfAccountTypeFromDocType(docType, AccMode.AR)
            AccTypeMode(accMode = AccMode.AR, accType = accTypeList)
        } else {
            accTypeList = getListOfAccountTypeFromDocType(docType, AccMode.AP)
            AccTypeMode(accMode = AccMode.AP, accType = accTypeList)
        }
    }

    private fun getListOfAccountTypeFromDocType(docType: String?, accMode: AccMode?): List<AccountType> {
        val jvList = settlementServiceHelper.getJvList(classType = AccountType::class.java)
        return when {
            docType == AresConstants.PAYMENT && accMode == AccMode.AR -> { listOf(AccountType.REC) }
            docType == AresConstants.PAYMENT && accMode == AccMode.AP -> { listOf(AccountType.PAY) }
            docType == AresConstants.INVOICE && accMode == AccMode.AR -> { listOf(AccountType.SINV) }
            docType == AresConstants.INVOICE && accMode == AccMode.AP -> { listOf(AccountType.PINV) }
            docType == AresConstants.INVOICE && accMode == null -> { listOf(AccountType.SINV, AccountType.PINV) }
            docType == AresConstants.CREDIT_NOTE && accMode == AccMode.AR -> { listOf(AccountType.SCN) }
            docType == AresConstants.CREDIT_NOTE && accMode == AccMode.AP -> { listOf(AccountType.PCN) }
            docType == AresConstants.JV && accMode != null -> { jvList }
            docType == null && accMode == AccMode.AR -> {
                listOf(AccountType.SINV, AccountType.REC, AccountType.SCN, AccountType.SDN) + jvList
            }
            docType == null && accMode == AccMode.AP -> {
                listOf(AccountType.PINV, AccountType.PCN, AccountType.PDN, AccountType.PAY) + jvList
            }
            docType == null && accMode == null -> { listOf(AccountType.SINV, AccountType.PINV) }
            else -> { emptyList() }
        }
    }

    /**
     * This private function is used to calculate total settled tds and outputs the amount in document currency
     * @param doc
     * @return BigDecimal
     */
    private suspend fun calculateSettledTds(doc: com.cogoport.ares.api.settlement.entity.Document): BigDecimal {
        return if (!doc.tdsCurrency.isNullOrBlank() && (doc.currency != doc.tdsCurrency)) {
            if (doc.ledCurrency == doc.tdsCurrency) {
                getExchangeValue(doc.settledTds, doc.exchangeRate, true)
            } else {
                val rate = fetchRateSettledTds(doc)
                getExchangeValue(doc.settledTds, rate)
            }
        } else {
            doc.settledTds
        }
    }

    /**
     * This is a private function to fetch exchange rate from the payments
     * @param tdsType
     * @param paymentIds
     * @return List<PaymentData>
     */
    private suspend fun getExchangeRateUsingPayment(
        tdsType: SettlementType?,
        paymentIds: List<Long>
    ): List<PaymentData> {
        val type = mutableListOf<SettlementType>()
        when (tdsType) {
            SettlementType.CTDS -> type.addAll(
                listOf(SettlementType.REC, SettlementType.SCN, SettlementType.SINV)
            )
            SettlementType.VTDS -> type.addAll(
                listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV)
            )
            else -> tdsType?.let { type.add(it) }
        }
        return accountUtilizationRepository.getPaymentDetails(paymentIds, type)
    }

    /**
     * This is a private function to fetch exchange rate to calculate settled tds
     * @param doc
     * @return BigDecimal
     */
    private suspend fun fetchRateSettledTds(doc: com.cogoport.ares.api.settlement.entity.Document): BigDecimal {
        val rate = doc.sourceId?.let {
            val rateList = getExchangeRateUsingPayment(doc.sourceType, listOf(it))
            if (rateList.isNotEmpty()) rateList[0].exchangeRate else null
        }

        if (rate == null) {
            doc.tdsCurrency?.let { it ->
                settlementServiceHelper.getExchangeRate(
                    it, doc.currency,
                    SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(doc.documentDate)
                )
            }
        }
        return rate ?: BigDecimal.ZERO
    }

    /**
     * Get TDS Rate from styles if present else return default 2%
     * @param tdsProfile
     * @return BigDecimal
     */
    private fun getTdsRate(
        tdsProfile: TdsStylesResponse?
    ): BigDecimal {
        return if (tdsProfile?.tdsDeductionType == "no_deduction") {
            AresConstants.NO_DEDUCTION_RATE.toBigDecimal()
        } else {
            tdsProfile?.tdsDeductionRate ?: AresConstants.DEFAULT_TDS_RATE.toBigDecimal()
        }
    }

    /**
     * Validate input for list of documents
     */
    private fun validateSettlementDocumentInput(request: SettlementDocumentRequest) {
        logger().info("ares.settlement.crossTradeParty: {}", crossTradeParty)
        if (request.entityCode == null) throw AresException(AresError.ERR_1003, "entityCode")
        if (request.importerExporterId == null && request.serviceProviderId == null)
            throw AresException(AresError.ERR_1003, "importerExporterId and serviceProviderId")
        if (!crossTradeParty &&
            request.importerExporterId != null &&
            request.serviceProviderId != null &&
            (request.importerExporterId != request.serviceProviderId)
        )
            throw AresException(AresError.ERR_1506, "")
    }

    /**
     * Get List of Documents from OpenSearch index_account_utilization
     * @param request
     * @return ResponseList
     */
    private suspend fun getTDSDocumentList(
        request: TdsSettlementDocumentRequest
    ): ResponseList<Document> {
        val offset =
            request.pageLimit?.let {
                (request.page?.let { request.pageLimit?.times(it) })?.minus(it)
            }
        val documentEntity =
            accountUtilizationRepository.getTDSDocumentList(
                request.pageLimit,
                offset,
                request.accType,
                request.orgId,
                request.accMode,
                request.startDate,
                request.endDate,
                "%${request.query}%"
            )
        if (documentEntity.isEmpty())
            return ResponseList()
        val tradePartyMappingIds = documentEntity
            .filter { document -> document!!.mappingId != null }
            .map { document -> document!!.mappingId.toString() }
            .distinct()
        val documentModel = groupDocumentList(documentEntity).map { documentConverter.convertToModel(it!!) }
        val total =
            accountUtilizationRepository.getTDSDocumentCount(
                request.accType,
                request.orgId,
                request.accMode,
                request.startDate,
                request.endDate,
                "%${request.query}%"
            )
        // Fetch Organization Tds Profile from mappingIds
        val tdsProfiles = listOrgTdsProfile(tradePartyMappingIds)
        for (doc in documentModel) {
            val tdsProfile = tdsProfiles.find { it.id == doc.mappingId }
            // Fetch Rate From Profile
            val rate = getTdsRate(tdsProfile)

            doc.documentType = settlementServiceHelper.getDocumentType(AccountType.valueOf(doc.documentType), doc.signFlag, doc.accMode)
            doc.status = settlementServiceHelper.getDocumentStatus(
                docAmount = doc.documentAmount,
                balanceAmount = doc.balanceAmount,
                docType = SettlementType.valueOf(doc.accountType)
            )
            doc.tds = calculateTds(
                rate = rate,
                settledTds = doc.settledTds!!,
                taxableAmount = doc.taxableAmount
            )
            doc.afterTdsAmount -= (doc.tds + doc.settledTds!!)
            doc.balanceAmount -= doc.tds
        }
        documentModel.forEach { it.documentNo = Hashids.encode(it.documentNo.toLong()) }
        return ResponseList(
            list = documentModel,
            totalPages = ceil(total?.toDouble()?.div(request.pageLimit!!) ?: 0.0).toLong(),
            totalRecords = total,
            pageNo = request.page
        )
    }

    private fun validateTdsDocumentInput(request: TdsSettlementDocumentRequest) {
        if (request.orgId.isEmpty()) throw AresException(AresError.ERR_1003, "orgId")
        if (request.accMode == null) throw AresException(AresError.ERR_1003, "account mode")
    }

    override suspend fun check(request: CheckRequest): CheckResponse {
        val stack = runSettlement(request, false)
        val canSettle = if (request.throughIncident) true else getCanSettleFlag(stack)
        return CheckResponse(
            stackDetails = stack,
            canSettle = canSettle
        )
    }

    private fun getCanSettleFlag(stack: List<CheckDocument>): Boolean {
        var canSettle = true
        val currencyList = stack.map { it.currency }
        if (currencyList.distinct().size > 1) canSettle = false
        if (canSettle) {
            stack.forEach {
                if (it.nostroAmount?.compareTo(BigDecimal.ZERO) != 0) canSettle = false
            }
        }
        return canSettle
    }

    override suspend fun editCheck(request: CheckRequest): CheckResponse {
        adjustBalanceAmount(type = "add", documents = request.stackDetails!!)
        val checkResponse = check(request)
        adjustBalanceAmount(type = "subtract", documents = request.stackDetails!!)
        return checkResponse
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun settle(request: CheckRequest): List<CheckDocument> {
        // If request is coming through incident management check
        return if (request.throughIncident) {
            // Validate Request
            if (request.incidentMappingId == null) throw AresException(AresError.ERR_1003, "incidentMappingId")
            if (request.incidentId == null) throw AresException(AresError.ERR_1003, "incidentId")

            // Update Status of Incident in incident_mappings table
            incidentMappingsRepository.updateStatus(
                incidentMappingId = Hashids.decode(request.incidentMappingId!!)[0],
                status = IncidentStatus.APPROVED
            )

            // Perform Settlement
            val response = runSettlement(request, true)

            // Update status of incident at incident management
            hadesClient.updateIncident(
                request = UpdateIncidentRequest(
                    status = com.cogoport.hades.model.incident.enums.IncidentStatus.APPROVED,
                    data = null,
                    remark = request.remark,
                    updatedBy = request.createdBy!!
                ),
                id = request.incidentId!!
            )

            // return response
            response
        } else {
            runSettlement(request, true)
        }
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun edit(request: CheckRequest): List<CheckDocument> = editSettlement(request)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun editTds(request: EditTdsRequest) = editInvoiceTds(request)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun delete(request: DeleteSettlementRequest) =
        deleteSettlement(request.documentNo, request.settlementType, request.deletedBy, request.deletedByUserType)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun sendForApproval(request: CreateIncidentRequest): String {
        val docList = request.stackDetails!!.map {
            documentConverter.convertToIncidentModel(it)
        }
        val formatedDate = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(request.settlementDate)
        val res = createIncidentMapping(
            accUtilIds = docList.map { Hashids.decode(it.id)[0] },
            data = docList,
            type = com.cogoport.ares.api.common.enums.IncidentType.SETTLEMENT_APPROVAL,
            status = IncidentStatus.REQUESTED,
            orgName = request.orgName,
            entityCode = request.entityCode,
            performedBy = request.createdBy
        )
        val incidentData =
            IncidentData(
                organization = Organization(
                    id = request.orgId,
                    businessName = request.orgName,
                    tradePartyType = null,
                    tradePartyName = null
                ),
                settlementRequest = com.cogoport.hades.model.incident.Settlement(
                    entityCode = request.entityCode!!,
                    list = docList,
                    settlementDate = java.sql.Date.valueOf(formatedDate),
                    incidentMappingId = res,
                    supportingDocUrl = request.supportingDocUrl
                ),
                tdsRequest = null,
                bankRequest = null,
                creditNoteRequest = null
            )
        hadesClient.createIncident(
            com.cogoport.hades.model.incident.request.CreateIncidentRequest(
                type = IncidentType.SETTLEMENT_APPROVAL,
                description = "Settlement Approval For Cross Currency Settle",
                data = incidentData,
                createdBy = request.createdBy!!
            )
        )

        return res
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun reject(request: RejectSettleApproval): String {
        incidentMappingsRepository.updateStatus(
            incidentMappingId = Hashids.decode(request.incidentMappingId!!)[0],
            status = IncidentStatus.REJECTED
        )
        hadesClient.updateIncident(
            request = UpdateIncidentRequest(
                status = com.cogoport.hades.model.incident.enums.IncidentStatus.REJECTED,
                data = null,
                remark = request.remark,
                updatedBy = request.performedBy!!
            ),
            id = request.incidentId!!
        )
        return request.incidentId!!
    }

    /**
     * Get Organization summary orgName, outstanding, currency, tdsStyle.
     * Used to display information on header of TDS Settlement.
     * @param: orgId
     * @param: startDate
     * @param: endDate
     * @return: endDate
     */
    override suspend fun getOrgSummary(request: OrgSummaryRequest): OrgSummaryResponse {
        val responseEntity =
            accountUtilizationRepository.getOrgSummary(request.orgId!!, request.accMode!!, request.startDate, request.endDate)
                ?: throw AresException(AresError.ERR_1005, "")
        val responseModel = orgSummaryConverter.convertToModel(responseEntity)
        val tdsResponse = getSelfOrgTdsProfile(request.orgId!!)
        val tdsStyle = TdsStyle(
            style = tdsResponse.tdsDeductionStyle,
            rate = tdsResponse.tdsDeductionRate,
            type = tdsResponse.tdsDeductionType
        )
        responseModel.tdsStyle = tdsStyle
        return responseModel
    }

    private suspend fun editInvoiceTds(request: EditTdsRequest): String {
        request.documentNo = Hashids.decode(request.documentNo!!)[0].toString()
        val doc =
            settlementRepository.findByDestIdAndDestType(
                request.documentNo!!.toLong(),
                request.settlementType!!
            )
        val tdsDoc =
            doc.find { it?.sourceType in listOf(SettlementType.CTDS, SettlementType.VTDS) }
                ?: throw AresException(AresError.ERR_1503, "TDS")
        val sourceDoc =
            doc.first { it.sourceType in fetchSettlingDocs(it?.destinationType!!) }
                ?: throw AresException(AresError.ERR_1503, AresConstants.PAYMENT)
        val sourceLedgerRate =
            Utilities.binaryOperation(sourceDoc.ledAmount, sourceDoc.amount!!, Operator.DIVIDE)
        var currNewTds = request.newTds!!
        if (sourceDoc.currency != request.currency) {
            val rate =
                if (sourceDoc.ledCurrency == request.currency) {
                    sourceLedgerRate
                } else {
                    val accUt = accountUtilizationRepository.findRecord(
                        sourceDoc.sourceId!!,
                        sourceDoc.sourceType.toString()
                    )
                        ?: throw AresException(
                            AresError.ERR_1503,
                            "${sourceDoc.sourceType}_${sourceDoc.sourceId}"
                        )
                    settlementServiceHelper.getExchangeRate(
                        sourceDoc.currency!!,
                        request.currency!!,
                        SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(accUt.transactionDate)
                    )
                }
            currNewTds = getExchangeValue(request.newTds!!, rate, true)
        }
        if (currNewTds > tdsDoc.amount!!) {
            // TODO("Generate Credit Note")
            return Hashids.encode(tdsDoc.destinationId)
        } else if (currNewTds < tdsDoc.amount) {
            val invoiceTdsDiff = request.oldTds!! - request.newTds!!
            val invoiceTdsDiffLed = Utilities.binaryOperation(invoiceTdsDiff, request.exchangeRate!!, Operator.MULTIPLY)
            val paymentTdsDiff = tdsDoc.amount!! - currNewTds

            reduceAccountUtilization(
                docId = tdsDoc.destinationId,
                accType = AccountType.valueOf(tdsDoc.destinationType.toString()),
                amount = invoiceTdsDiff,
                ledAmount = invoiceTdsDiffLed,
                updatedBy = request.updatedBy!!,
                updatedByUserType = request.updatedByUserType,
                tdsPaid = invoiceTdsDiff
            )
            sourceDoc.amount = sourceDoc.amount?.minus(paymentTdsDiff)
            sourceDoc.ledAmount =
                sourceDoc.ledAmount.minus(
                    Utilities.binaryOperation(
                        paymentTdsDiff,
                        sourceLedgerRate,
                        Operator.MULTIPLY
                    )
                )
            sourceDoc.updatedBy = request.updatedBy
            sourceDoc.updatedAt = Timestamp.from(Instant.now())
            settlementRepository.update(sourceDoc)
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.SETTLEMENT,
                    objectId = sourceDoc.id,
                    actionName = AresConstants.UPDATE,
                    data = sourceDoc,
                    performedBy = request.updatedBy.toString(),
                    performedByUserType = request.updatedByUserType
                )
            )
        }
        tdsDoc.amount = currNewTds
        tdsDoc.ledAmount =
            Utilities.binaryOperation(currNewTds, sourceLedgerRate, Operator.MULTIPLY)
        tdsDoc.updatedBy = request.updatedBy
        tdsDoc.updatedAt = Timestamp.from(Instant.now())
        settlementRepository.update(tdsDoc)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.SETTLEMENT,
                objectId = tdsDoc.id,
                actionName = AresConstants.UPDATE,
                data = tdsDoc,
                performedBy = request.updatedBy.toString(),
                performedByUserType = request.updatedByUserType
            )
        )
        return Hashids.encode(tdsDoc.destinationId)
    }

    /**
     * This private suspended function edits the past settlement.
     * @param request
     * @return List<CheckDocument>
     */
    private suspend fun editSettlement(request: CheckRequest): List<CheckDocument> {
        val sourceDoc = getSourceDocFromStack(request.stackDetails!!) ?: throw AresException(AresError.ERR_1501, "")
        deleteSettlement(sourceDoc.documentNo, sourceDoc.accountType, request.createdBy!!, request.createdByUserType)
        return runSettlement(request, true)
    }

    /**
     * This private function returns the source document from the stack.
     * @param stack
     * @return CheckDocument?
     */
    private fun getSourceDocFromStack(stack: List<CheckDocument>): CheckDocument? {
        val accTypesInStack = stack.map { it.accountType }
        return if (SettlementType.SINV in accTypesInStack && SettlementType.PINV in accTypesInStack) {
            stack.find { it.accountType == SettlementType.SINV }
        } else {
            stack.find {
                it.accountType in listOf(SettlementType.REC, SettlementType.PCN, SettlementType.PAY, SettlementType.SCN)
            }
        }
    }

    private suspend fun deleteSettlement(documentNo: String, settlementType: SettlementType, deletedBy: UUID, deletedByUserType: String?): String {
        val documentNo = Hashids.decode(documentNo)[0]
        val sourceType =
            when (settlementType) {
                SettlementType.REC -> listOf(SettlementType.REC, SettlementType.CTDS, SettlementType.SECH, SettlementType.NOSTRO)
                SettlementType.PAY -> listOf(SettlementType.PAY, SettlementType.VTDS, SettlementType.PECH, SettlementType.NOSTRO)
                SettlementType.SINV -> listOf(SettlementType.SINV, SettlementType.CTDS, SettlementType.VTDS, SettlementType.SECH, SettlementType.PECH, SettlementType.NOSTRO)
                SettlementType.SCN -> listOf(SettlementType.SCN, SettlementType.CTDS, SettlementType.SECH, SettlementType.NOSTRO)
                else -> listOf(SettlementType.PCN, SettlementType.VTDS, SettlementType.PECH, SettlementType.NOSTRO)
            }
        val fetchedDoc = settlementRepository.findBySourceIdAndSourceType(documentNo, sourceType)
        val paymentTdsDoc = fetchedDoc.find { it?.destinationId == documentNo }
        val debitDoc = fetchedDoc.filter { it?.destinationId != documentNo }.groupBy { it?.destinationId }
        val sourceCurr =
            fetchedDoc.sumOf {
                it?.amount?.multiply(BigDecimal.valueOf(it.signFlag.toLong()))
                    ?: BigDecimal.ZERO
            }
        reduceAccountUtilization(
            docId = documentNo,
            accType = AccountType.valueOf(settlementType.toString()),
            amount = sourceCurr,
            updatedBy = deletedBy,
            updatedByUserType = deletedByUserType,
            tdsPaid = paymentTdsDoc?.amount
        )
        for (debits in debitDoc) {
            val settledDoc =
                debits.value.filter { it?.sourceType == settlementType }
            val tdsDoc = debits.value.find { it?.sourceType in listOf(SettlementType.CTDS, SettlementType.VTDS) }
            var tdsPaid = tdsDoc?.amount ?: BigDecimal.ZERO
            if (settledDoc.isEmpty()) throw AresException(AresError.ERR_1501, "")
            for (source in settledDoc) {
                val payment =
                    accountUtilizationRepository.findRecord(
                        source!!.sourceId!!,
                        source.sourceType.toString()
                    )
                        ?: throw AresException(
                            AresError.ERR_1503,
                            source.sourceId.toString()
                        )
                val invoice =
                    accountUtilizationRepository.findRecord(
                        source.destinationId,
                        source.destinationType.toString()
                    )
                        ?: throw AresException(
                            AresError.ERR_1503,
                            source.destinationId.toString()
                        )
                var settledCurr = source.amount!!
                if (payment.currency != invoice.currency) {
                    val rate =
                        if (payment.ledCurrency == invoice.currency) {
                            Utilities.binaryOperation(
                                payment.amountLoc,
                                payment.amountCurr,
                                Operator.DIVIDE
                            )
                        } else {
                            settlementServiceHelper.getExchangeRate(
                                payment.currency,
                                invoice.currency,
                                SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(payment.transactionDate)
                            )
                        }
                    settledCurr = getExchangeValue(settledCurr, rate)
                    tdsPaid = getExchangeValue(tdsPaid, rate)
                }
                reduceAccountUtilization(
                    docId = source.destinationId,
                    accType = AccountType.valueOf(source.destinationType.toString()),
                    amount = settledCurr,
                    updatedBy = deletedBy,
                    updatedByUserType = deletedByUserType,
                    tdsPaid = tdsPaid
                )
            }
        }
        val settlements = settlementRepository.findByIdIn(fetchedDoc.map { it?.id!! })
        settlementRepository.deleteByIdIn(fetchedDoc.map { it?.id!! })
        for (settlementDoc in settlements) {
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.SETTLEMENT,
                    objectId = settlementDoc.id,
                    actionName = AresConstants.DELETE,
                    data = settlementDoc,
                    performedBy = deletedBy.toString(),
                    performedByUserType = deletedByUserType
                )
            )
        }

        return Hashids.encode(documentNo)
    }

    private suspend fun reduceAccountUtilization(
        docId: Long,
        accType: AccountType,
        amount: BigDecimal,
        ledAmount: BigDecimal? = null,
        updatedBy: UUID,
        updatedByUserType: String? = null,
        tdsPaid: BigDecimal? = null
    ) {
        val accUtil =
            accountUtilizationRepository.findRecord(docId, accType.toString())
                ?: throw AresException(AresError.ERR_1503, "${accType}_$docId")
        if ((accUtil.payCurr - amount).compareTo(BigDecimal.ZERO) == 0) {
            accUtil.payCurr = BigDecimal.ZERO
            accUtil.payLoc = BigDecimal.ZERO
        } else {
            accUtil.payCurr -= amount
            accUtil.payLoc -=
                ledAmount
                    ?: getExchangeValue(
                        amount,
                        Utilities.binaryOperation(
                            accUtil.amountLoc,
                            accUtil.amountCurr,
                            Operator.DIVIDE
                        )
                    )
        }
        accUtil.updatedAt = Timestamp.from(Instant.now())
        val accUtilObj = accountUtilizationRepository.update(accUtil)
        try {
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                    objectId = accUtilObj.id,
                    actionName = AresConstants.UPDATE,
                    data = accUtilObj,
                    performedBy = updatedBy.toString(),
                    performedByUserType = updatedByUserType
                )
            )
            val paidTds = (tdsPaid ?: BigDecimal.ZERO) * (-1).toBigDecimal()
            updateExternalSystemInvoice(accUtilObj, paidTds, updatedBy, updatedByUserType)
            OpenSearchClient().updateDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, accUtilObj.id.toString(), accUtilObj)
            emitDashboardAndOutstandingEvent(accUtilObj)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
    }

    private suspend fun runSettlement(
        request: CheckRequest,
        performDbOperation: Boolean
    ): List<CheckDocument> {
        val settledTdsCopy = storeSettledTds(request)
        sanitizeInput(request)
        val source = mutableListOf<CheckDocument>()
        val dest = mutableListOf<CheckDocument>()
        val creditType =
            listOf(
                SettlementType.REC,
                SettlementType.PCN,
                SettlementType.PAY,
                SettlementType.SCN
            )
        val debitType =
            listOf(
                SettlementType.SINV,
                SettlementType.PINV,
                SettlementType.SDN,
                SettlementType.PDN,
                SettlementType.PREIMB,
                SettlementType.SREIMB
            )
        val jvType =
            listOf(
                SettlementType.WOFF,
                SettlementType.ROFF,
                SettlementType.JVNOS,
                SettlementType.EXCH,
                SettlementType.OUTST
            )
        for (doc in request.stackDetails!!.reversed()) {
            if (creditType.contains(doc.accountType)) {
                source.add(doc)
            } else if (debitType.contains(doc.accountType)) {
                dest.add(doc)
            } else if (jvType.contains(doc.accountType)) {
                when (doc.accMode) {
                    AccMode.AR -> {
                        when (doc.signFlag.toInt()) {
                            1 -> dest.add(doc)
                            -1 -> source.add(doc)
                        }
                    }
                    AccMode.AP -> {
                        when (doc.signFlag.toInt()) {
                            1 -> source.add(doc)
                            -1 -> dest.add(doc)
                        }
                    }
                }
            }
        }
        if (source.isEmpty() &&
            (
                dest.map { it.accountType }.contains(SettlementType.SINV) &&
                    dest.map { it.accountType }.contains(SettlementType.PINV)
                ) ||
            (
                dest.map { it.accountType }.contains(SettlementType.SREIMB) &&
                    dest.map { it.accountType }.contains(SettlementType.PREIMB)
                )
        ) {

            val allowedSettlementType = mutableListOf<SettlementType>(SettlementType.SREIMB, SettlementType.SINV)
            val res = dest.filter { it -> allowedSettlementType.contains(it.accountType) }.forEach {
                source.add(it)
                dest.remove(it)
            }
        }
        businessValidation(source, dest)
        if (source.any { it.hasPayrun } || dest.any { it.hasPayrun }) {
            AresException(AresError.ERR_1512, "")
        }
        val settledList = settleDocuments(request, source, dest, performDbOperation)
        settledList.forEach {
            it.id = Hashids.encode(it.id.toLong())
            it.documentNo = Hashids.encode(it.documentNo.toLong())
            it.settledTds = settledTdsCopy[it.id]!!
        }
        return request.stackDetails!!.map { r -> settledList.filter { it.id == r.id }[0] }
    }

    /**
     * Settle documents: source to destination.
     */
    private suspend fun settleDocuments(
        request: CheckRequest,
        source: MutableList<CheckDocument>,
        dest: MutableList<CheckDocument>,
        performDbOperation: Boolean
    ): MutableList<CheckDocument> {
        val response = mutableListOf<CheckDocument>()
        for (payment in source) {
            var availableAmount = payment.allocationAmount
            val canSettle = fetchSettlingDocs(payment.accountType)
            for (invoice in dest) {
                if (canSettle.contains(invoice.accountType)) {
                    availableAmount =
                        doSettlement(
                            request,
                            invoice,
                            availableAmount,
                            payment,
                            source,
                            performDbOperation
                        )
                }
                if (payment.tds!!.compareTo(BigDecimal.ZERO) != 0 &&
                    payment.settledTds.compareTo(BigDecimal.ZERO) == 0 &&
                    performDbOperation
                ) {
                    createTdsRecord(
                        sourceId = payment.documentNo.toLong(),
                        destId = payment.documentNo.toLong(),
                        destType = payment.accountType,
                        currency = payment.currency,
                        ledCurrency = payment.ledCurrency,
                        tdsAmount = payment.tds!!,
                        tdsLedAmount = getExchangeValue(payment.tds!!, payment.exchangeRate),
                        settlementDate = request.settlementDate,
                        signFlag = 1,
                        createdBy = request.createdBy,
                        createdByUserType = request.createdByUserType,
                        supportingDocUrl = request.supportingDocUrl
                    )
                    payment.settledTds += payment.tds!!
                }
            }
            payment.allocationAmount -= availableAmount
            payment.balanceAfterAllocation =
                payment.balanceAmount.subtract(payment.allocationAmount)
            assignPaymentStatus(payment)
            response.add(payment)
        }
        dest.forEach { response.add(it) }
        return response
    }

    private suspend fun doSettlement(
        request: CheckRequest,
        invoice: CheckDocument,
        availableAmount: BigDecimal,
        payment: CheckDocument,
        source: MutableList<CheckDocument>,
        performDbOperation: Boolean
    ): BigDecimal {
        var amount = availableAmount
        val toSettleAmount = invoice.allocationAmount - invoice.settledAllocation
        if (toSettleAmount.compareTo(BigDecimal.ZERO) != 0) {
            var rate = 1.toBigDecimal()
            val ledgerRate = payment.exchangeRate
            var updateDoc = true
            if (payment.currency != invoice.currency) {
                rate =
                    if (payment.ledCurrency == invoice.currency) {
                        ledgerRate
                    } else {
                        settlementServiceHelper.getExchangeRate(
                            payment.currency,
                            invoice.currency,
                            SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(payment.transactionDate)
                        )
                    }
                amount = getExchangeValue(availableAmount, rate)
            }
            if (amount >= toSettleAmount) {
                amount =
                    updateDocuments(
                        request,
                        invoice,
                        payment,
                        toSettleAmount,
                        amount,
                        rate,
                        ledgerRate,
                        true,
                        performDbOperation
                    )
            } else if (amount < toSettleAmount) {
                if (payment != source.last()) updateDoc = false
                amount =
                    updateDocuments(
                        request,
                        invoice,
                        payment,
                        amount,
                        amount,
                        rate,
                        ledgerRate,
                        updateDoc,
                        performDbOperation
                    )
            }
        }
        return amount
    }

    private suspend fun updateDocuments(
        request: CheckRequest,
        invoice: CheckDocument,
        payment: CheckDocument,
        toSettleAmount: BigDecimal,
        availableAmount: BigDecimal,
        exchangeRate: BigDecimal,
        ledgerRate: BigDecimal,
        updateDoc: Boolean,
        performDbOperation: Boolean
    ): BigDecimal {
        val amount = availableAmount - toSettleAmount
        invoice.settledAllocation += toSettleAmount
        payment.settledAllocation +=
            getExchangeValue(toSettleAmount, exchangeRate, true)
        if (updateDoc) {
            invoice.allocationAmount = invoice.settledAllocation
            invoice.balanceAfterAllocation =
                invoice.balanceAmount.subtract(invoice.allocationAmount)
        }
        assignInvoiceStatus(invoice)
        assignPaymentStatus(payment)
        if (performDbOperation && toSettleAmount.compareTo(BigDecimal.ZERO) != 0)
            performDbOperation(
                request,
                toSettleAmount,
                exchangeRate,
                ledgerRate,
                payment,
                invoice
            )
        return getExchangeValue(amount, exchangeRate, true)
    }

    private suspend fun performDbOperation(
        request: CheckRequest,
        toSettleAmount: BigDecimal,
        exchangeRate: BigDecimal,
        ledgerRate: BigDecimal,
        payment: CheckDocument,
        invoice: CheckDocument
    ) {
        val jvList = settlementServiceHelper.getJvList(SettlementType::class.java)

        /** Invoice Amount in payment currency */
        val paidAmount = getExchangeValue(toSettleAmount, exchangeRate, true)
        /** Payment ledger amount */
        val paidLedAmount = getExchangeValue(paidAmount, ledgerRate)
        /** Tds Amount in Invoice currency */
        val invoiceTds = invoice.tds!! - invoice.settledTds
        /** Tds Amount in Payment currency */
        val paymentTds = getExchangeValue(invoiceTds, exchangeRate, true)
        /** Payment Tds ledger Amount */
        val paymentTdsLed = getExchangeValue(paymentTds, ledgerRate)
        /** Nostro Amount in Invoice currency */
        val invoiceNostro = invoice.nostroAmount!! - invoice.settledNostro!!
        /** Nostro Amount in payment currency */
        val paymentNostro = getExchangeValue(invoiceNostro, exchangeRate, true)
        /** Payment Nostro Ledger Amount */
        val paymentNostroLed = getExchangeValue(paymentNostro, ledgerRate)

        val isNotJv = payment.accountType !in jvList

        val amount = paidAmount + if (isNotJv) paymentTds + paymentNostro else BigDecimal.ZERO
        val ledAmount = paidLedAmount + if (isNotJv) paymentTdsLed + paymentNostroLed else BigDecimal.ZERO
        // Create Documents Settlement Entry
        if (amount.compareTo(BigDecimal.ZERO) != 0) {
            createSettlement(
                payment.documentNo.toLong(),
                payment.accountType,
                invoice.documentNo.toLong(),
                invoice.accountType,
                payment.currency,
                amount,
                payment.ledCurrency,
                ledAmount,
                1,
                request.settlementDate,
                request.createdBy,
                request.createdByUserType,
                request.supportingDocUrl
            )
        }

        // Create TDS Entry
        if (paymentTds.compareTo(BigDecimal.ZERO) != 0 && (isNotJv)) {
            createTdsRecord(
                sourceId = payment.documentNo.toLong(),
                destId = invoice.documentNo.toLong(),
                destType = invoice.accountType,
                currency = payment.currency,
                ledCurrency = payment.ledCurrency,
                tdsAmount = paymentTds,
                tdsLedAmount = paymentTdsLed,
                settlementDate = request.settlementDate,
                signFlag = -1,
                createdBy = request.createdBy,
                createdByUserType = request.createdByUserType,
                supportingDocUrl = request.supportingDocUrl
            )
            invoice.settledTds += invoiceTds
        }

        // Create Nostro Entry
        if (paymentNostro.compareTo(BigDecimal.ZERO) != 0 && (isNotJv)) {
            createSettlement(
                sourceId = payment.documentNo.toLong(),
                sourceType = SettlementType.NOSTRO,
                destId = invoice.documentNo.toLong(),
                destType = invoice.accountType,
                currency = payment.currency,
                amount = paymentNostro,
                ledCurrency = payment.ledCurrency,
                ledAmount = paymentNostroLed,
                signFlag = -1,
                transactionDate = request.settlementDate,
                createdBy = request.createdBy,
                createdByUserType = request.createdByUserType,
                supportingDocUrl = request.supportingDocUrl
            )
            invoice.settledNostro = invoice.settledNostro!! + invoiceNostro
        }

        // Create Exchange Loss Gain Entry
        if (payment.ledCurrency != invoice.currency) {
            val excLedAmount =
                getExchangeValue(toSettleAmount, invoice.exchangeRate) - (paidLedAmount)
            if (excLedAmount.compareTo(BigDecimal.ZERO) != 0) {
                val exType =
                    if (invoice.accMode == AccMode.AR)
                        SettlementType.SECH
                    else SettlementType.PECH
                val exSign =
                    excLedAmount.signum() *
                        if (payment.accountType in listOf(
                                SettlementType.SCN,
                                SettlementType.REC,
                                SettlementType.SINV
                            )
                        ) {
                            -1
                        } else {
                            1
                        }
                createSettlement(
                    payment.documentNo.toLong(),
                    exType,
                    invoice.documentNo.toLong(),
                    invoice.accountType,
                    null,
                    null,
                    invoice.ledCurrency,
                    excLedAmount.abs(),
                    exSign.toShort(),
                    request.settlementDate,
                    request.createdBy,
                    request.createdByUserType,
                    request.supportingDocUrl
                )
            }
        }

        // Update Documents in Account Utilization
        val utilizedTdsOfPaymentDoc =
            if (payment.accountType in listOf(SettlementType.SINV, SettlementType.PCN, SettlementType.SCN))
                (payment.tds ?: BigDecimal.ZERO) - payment.settledTds
            else BigDecimal.ZERO
        val paymentUtilized = paidAmount + utilizedTdsOfPaymentDoc
        val invoiceUtilized = toSettleAmount + if (isNotJv) invoiceTds + invoiceNostro else BigDecimal.ZERO
        updateAccountUtilization(payment, paymentUtilized, utilizedTdsOfPaymentDoc, request.createdBy!!, request.createdByUserType) // Update Payment
        updateAccountUtilization(invoice, invoiceUtilized, invoiceTds, request.createdBy!!, request.createdByUserType) // Update Invoice
    }

    private suspend fun createTdsRecord(
        sourceId: Long?,
        destId: Long,
        destType: SettlementType,
        currency: String?,
        ledCurrency: String,
        tdsAmount: BigDecimal,
        tdsLedAmount: BigDecimal,
        signFlag: Short,
        settlementDate: Timestamp,
        createdBy: UUID?,
        createdByUserType: String?,
        supportingDocUrl: String?
    ) {
        val tdsType =
            if (fetchSettlingDocs(SettlementType.CTDS).contains(destType)) {
                SettlementType.CTDS
            } else {
                SettlementType.VTDS
            }
        createSettlement(
            sourceId,
            tdsType,
            destId,
            destType,
            currency,
            tdsAmount,
            ledCurrency,
            tdsLedAmount,
            signFlag,
            settlementDate,
            createdBy,
            createdByUserType,
            supportingDocUrl
        )
    }

    private suspend fun updateAccountUtilization(
        document: CheckDocument,
        utilizedAmount: BigDecimal,
        paidTds: BigDecimal,
        updatedBy: UUID,
        updatedByUserType: String?
    ) {
        val paymentUtilization =
            accountUtilizationRepository.findRecord(
                document.documentNo.toLong(),
                document.accountType.toString()
            )
                ?: throw AresException(
                    AresError.ERR_1503,
                    "${document.documentNo}_${document.accountType}"
                )
        if ((paymentUtilization.amountCurr - paymentUtilization.payCurr) < utilizedAmount.setScale(AresConstants.ROUND_DECIMAL_TO, RoundingMode.DOWN)) {
            throw AresException(AresError.ERR_1504, " Document No: ${paymentUtilization.documentValue}")
        }
        paymentUtilization.payCurr += utilizedAmount
        paymentUtilization.payLoc += getExchangeValue(utilizedAmount, document.exchangeRate)
        paymentUtilization.updatedAt = Timestamp.from(Instant.now())
        val accountUtilization = accountUtilizationRepository.update(paymentUtilization)
        try {
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                    objectId = accountUtilization.id,
                    actionName = AresConstants.UPDATE,
                    data = accountUtilization,
                    performedBy = updatedBy.toString(),
                    performedByUserType = updatedByUserType
                )
            )
            updateExternalSystemInvoice(accountUtilization, paidTds, updatedBy, updatedByUserType)
            OpenSearchClient().updateDocument(AresConstants.ACCOUNT_UTILIZATION_INDEX, paymentUtilization.id.toString(), paymentUtilization)
            emitDashboardAndOutstandingEvent(paymentUtilization)
        } catch (e: Exception) {
            logger().error(e.stackTraceToString())
        }
    }

    /**
     * Invokes Kafka for Plutus(Sales) or Kuber(Purchase) based on accountType in accountUtilization.
     * @param: accountUtilization
     */
    private suspend fun updateExternalSystemInvoice(
        accountUtilization: AccountUtilization,
        paidTds: BigDecimal,
        performedBy: UUID,
        performedByUserType: String?
    ) {
        when (accountUtilization.accType) {
            AccountType.PINV, AccountType.PCN -> emitPayableBillStatus(accountUtilization, paidTds, performedBy, performedByUserType)
            AccountType.SINV, AccountType.SCN -> updateBalanceAmount(accountUtilization, performedBy, performedByUserType)
            AccountType.EXCH, AccountType.ROFF, AccountType.OUTST, AccountType.WOFF, AccountType.JVNOS ->
                journalVoucherService.updateJournalVoucherStatus(
                    id = accountUtilization.documentNo,
                    status = JVStatus.UTILIZED,
                    performedBy = performedBy,
                    performedByUserType = performedByUserType
                )
            else -> {}
        }
    }

    /**
     * Invokes Kafka event to update balanceAmount in Plutus(Sales MS).
     * @param: accountUtilization
     */
    private fun updateBalanceAmount(
        accountUtilization: AccountUtilization,
        performedBy: UUID,
        performedByUserType: String?
    ) {
        aresKafkaEmitter.emitInvoiceBalance(
            invoiceBalanceEvent = UpdateInvoiceBalanceEvent(
                invoiceBalance = InvoiceBalance(
                    invoiceId = accountUtilization.documentNo,
                    balanceAmount = accountUtilization.amountCurr - accountUtilization.payCurr,
                    performedBy = performedBy,
                    performedByUserType = performedByUserType,
                    paymentStatus = Utilities.getPaymentStatus(accountUtilization)
                )
            )
        )
    }

    /**
     * Invokes Kafka event to update status in Kuber(Purchase MS).
     * @param: accountUtilization
     */
    private fun emitPayableBillStatus(
        accountUtilization: AccountUtilization,
        paidTds: BigDecimal,
        performedBy: UUID?,
        performedByUserType: String?
    ) {
        val status = if (accountUtilization.payLoc.compareTo(BigDecimal.ZERO) == 0)
            "UNPAID"
        else if (accountUtilization.amountCurr > accountUtilization.payCurr)
            "PARTIAL"
        else
            "FULL"

        aresKafkaEmitter.emitUpdateBillPaymentStatus(
            UpdatePaymentStatusRequest(
                billId = accountUtilization.documentNo,
                paymentStatus = status,
                paidAmount = accountUtilization.payCurr,
                paidTds = paidTds,
                performedBy = performedBy,
                performedByUserType = performedByUserType
            )
        )
    }

    private fun emitDashboardAndOutstandingEvent(
        accUtilizationRequest: AccountUtilization
    ) {
        emitDashboardData(accUtilizationRequest)
        if (accUtilizationRequest.accMode == AccMode.AR) {
            emitOutstandingData(accUtilizationRequest)
        }
    }

    private fun emitDashboardData(accUtilizationRequest: AccountUtilization) {
        val date: Date = accUtilizationRequest.transactionDate!!
        aresKafkaEmitter.emitDashboardData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    date = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(date),
                    quarter = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        .get(IsoFields.QUARTER_OF_YEAR),
                    year = date.toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDate().year,
                    accMode = accUtilizationRequest.accMode,
                    serviceType = if (accUtilizationRequest.serviceType.isNullOrBlank()) null else ServiceType.valueOf(accUtilizationRequest.serviceType.uppercase()),
                    invoiceCurrency = accUtilizationRequest.currency
                )
            )
        )
    }

    private fun emitOutstandingData(accUtilizationRequest: AccountUtilization) {
        aresKafkaEmitter.emitOutstandingData(
            OpenSearchEvent(
                OpenSearchRequest(
                    zone = accUtilizationRequest.zoneCode,
                    orgId = accUtilizationRequest.organizationId.toString(),
                    orgName = accUtilizationRequest.organizationName,
                    serviceType = if (accUtilizationRequest.serviceType.isNullOrBlank()) null else ServiceType.valueOf(accUtilizationRequest.serviceType.uppercase()),
                    invoiceCurrency = accUtilizationRequest.currency
                )
            )
        )
    }

    private suspend fun createSettlement(
        sourceId: Long?,
        sourceType: SettlementType,
        destId: Long,
        destType: SettlementType,
        currency: String?,
        amount: BigDecimal?,
        ledCurrency: String,
        ledAmount: BigDecimal,
        signFlag: Short,
        transactionDate: Timestamp,
        createdBy: UUID?,
        createdByUserType: String?,
        supportingDocUrl: String?
    ) {
        val settledDoc =
            Settlement(
                null,
                sourceId,
                sourceType,
                destId,
                destType,
                currency,
                amount,
                ledCurrency,
                ledAmount,
                signFlag,
                transactionDate,
                createdBy,
                Timestamp.from(Instant.now()),
                createdBy,
                Timestamp.from(Instant.now()),
                supportingDocUrl
            )
        val settleDoc = settlementRepository.save(settledDoc)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.SETTLEMENT,
                objectId = settleDoc.id,
                actionName = AresConstants.CREATE,
                data = settleDoc,
                performedBy = createdBy.toString(),
                performedByUserType = createdByUserType
            )
        )
    }

    private fun getExchangeValue(
        amount: BigDecimal,
        exchangeRate: BigDecimal,
        reverse: Boolean = false
    ): BigDecimal {
        return if (reverse) {
            Utilities.binaryOperation(amount, exchangeRate, Operator.DIVIDE)
        } else {
            Utilities.binaryOperation(amount, exchangeRate, Operator.MULTIPLY)
        }
    }

    private fun fetchSettlingDocs(accType: SettlementType): List<SettlementType> {
        val jvSettleList = listOf(SettlementType.SINV, SettlementType.PINV, SettlementType.REC, SettlementType.PAY, SettlementType.SREIMB, SettlementType.PREIMB)
        val jvList = settlementServiceHelper.getJvList(classType = SettlementType::class.java)
        return when (accType) {
            SettlementType.REC -> {
                listOf(SettlementType.SINV, SettlementType.SDN) + jvList
            }
            SettlementType.PINV -> {
                listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV) + jvList
            }
            SettlementType.PCN -> {
                listOf(SettlementType.PINV, SettlementType.PDN)
            }
            SettlementType.PAY -> {
                listOf(SettlementType.PINV, SettlementType.PDN) + jvList
            }
            SettlementType.SINV -> {
                listOf(SettlementType.REC, SettlementType.SCN, SettlementType.PINV) + jvList
            }
            SettlementType.SCN -> {
                listOf(SettlementType.SINV, SettlementType.SDN)
            }
            SettlementType.SDN -> {
                listOf(SettlementType.SCN, SettlementType.REC)
            }
            SettlementType.PDN -> {
                listOf(SettlementType.PCN, SettlementType.PAY)
            }
            SettlementType.CTDS -> {
                listOf(SettlementType.SINV, SettlementType.SDN, SettlementType.SCN)
            }
            SettlementType.VTDS -> {
                listOf(SettlementType.PINV, SettlementType.PDN, SettlementType.PCN)
            }
            SettlementType.WOFF -> {
                jvSettleList
            }
            SettlementType.ROFF -> {
                jvSettleList
            }
            SettlementType.OUTST -> {
                jvSettleList
            }
            SettlementType.EXCH -> {
                jvSettleList
            }
            SettlementType.JVNOS -> {
                jvSettleList
            }
            SettlementType.PREIMB -> {
                listOf(SettlementType.SREIMB)
            }
            SettlementType.SREIMB -> {
                listOf(SettlementType.PREIMB)
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun storeSettledTds(request: CheckRequest): MutableMap<String, BigDecimal> {
        val settledTdsCopy = mutableMapOf<String, BigDecimal>()
        request.stackDetails!!.forEach {
            settledTdsCopy.put(it.id, it.settledTds)
        }
        return settledTdsCopy
    }

    private fun sanitizeInput(request: CheckRequest) {
        request.stackDetails!!.forEach {
            it.id = Hashids.decode(it.id)[0].toString()
            it.documentNo = Hashids.decode(it.documentNo)[0].toString()
            it.settledAllocation = BigDecimal.ZERO
            it.settledTds = BigDecimal.ZERO
            it.settledNostro = BigDecimal.ZERO
            if (it.nostroAmount == null) it.nostroAmount = BigDecimal.ZERO
            if (it.tds == null) it.tds = BigDecimal.ZERO
        }
    }

    /**
     * Run business Validations on source and destination document list.
     * @param: source
     * @param: dest
     */
    private fun businessValidation(
        source: MutableList<CheckDocument>,
        dest: MutableList<CheckDocument>
    ) {
        var creditCount = 0
        var debitCount = 0
        for (payment in source) {
            fetchSettlingDocs(payment.accountType).forEach { debit ->
                if (dest.map { it.accountType }.contains(debit)) debitCount += 1
            }
            if (debitCount == 0) throw AresException(AresError.ERR_1502, "") else debitCount = 0
        }
        for (invoice in dest) {
            fetchSettlingDocs(invoice.accountType).forEach { credit ->
                if (source.map { it.accountType }.contains(credit)) creditCount += 1
            }
            if (creditCount == 0) throw AresException(AresError.ERR_1501, "") else creditCount = 0
        }
    }

    private fun assignInvoiceStatus(doc: CheckDocument) {
        if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) == 0) {
            doc.status = DocStatus.KNOCKED_OFF.value
        } else if (decimalRound(doc.settledAllocation).compareTo(0.toBigDecimal()) == 0) {
            doc.status = DocStatus.UNPAID.value
        } else if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) == 1) {
            doc.status = DocStatus.PARTIAL_PAID.value
        }
    }

    private fun assignPaymentStatus(doc: CheckDocument) {
        if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) == 0) {
            doc.status = DocStatus.UTILIZED.value
        } else if (decimalRound(doc.settledAllocation).compareTo(0.toBigDecimal()) == 0) {
            doc.status = DocStatus.UNUTILIZED.value
        } else if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) == 1) {
            doc.status = DocStatus.PARTIAL_UTILIZED.value
        }
    }

    private fun decimalRound(amount: BigDecimal): BigDecimal {
        return Utilities.decimalRound(amount)
    }

    private fun adjustBalanceAmount(type: String, documents: List<CheckDocument>): List<CheckDocument> {
        if (type == "add") {
            documents.forEach {
                it.balanceAmount += (it.settledAmount) ?: BigDecimal.ZERO
            }
        }
        if (type == "subtract") {
            documents.forEach {
                it.balanceAmount -= (it.settledAmount) ?: BigDecimal.ZERO
            }
        }
        return documents
    }

    private suspend fun createIncidentMapping(
        accUtilIds: List<Long>?,
        data: Any?,
        type: com.cogoport.ares.api.common.enums.IncidentType?,
        status: IncidentStatus?,
        orgName: String?,
        entityCode: Int?,
        performedBy: UUID?
    ): String {
        val incMapObj = IncidentMappings(
            id = null,
            accountUtilizationIds = accUtilIds,
            data = data,
            incidentType = type,
            incidentStatus = status,
            organizationName = orgName,
            entityCode = entityCode,
            createdBy = performedBy,
            updatedBy = performedBy,
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now())
        )
        val savedObj = incidentMappingsRepository.save(incMapObj)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.INCIDENT_MAPPINGS,
                objectId = savedObj.id,
                actionName = AresConstants.CREATE,
                data = savedObj,
                performedBy = performedBy.toString(),
                performedByUserType = null
            )
        )
        return Hashids.encode(savedObj.id!!)
    }
}
