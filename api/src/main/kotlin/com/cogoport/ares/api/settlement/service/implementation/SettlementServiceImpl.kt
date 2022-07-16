package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.mapper.DocumentMapper
import com.cogoport.ares.api.settlement.mapper.HistoryDocumentMapper
import com.cogoport.ares.api.settlement.mapper.OrgSummaryMapper
import com.cogoport.ares.api.settlement.mapper.SettledInvoiceMapper
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.SettlementService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.InvoiceStatus
import com.cogoport.ares.model.payment.InvoiceType
import com.cogoport.ares.model.payment.Operator
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.CheckRequest
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.EditTdsRequest
import com.cogoport.ares.model.settlement.HistoryDocument
import com.cogoport.ares.model.settlement.Invoice
import com.cogoport.ares.model.settlement.OrgSummaryResponse
import com.cogoport.ares.model.settlement.SettlementDocumentRequest
import com.cogoport.ares.model.settlement.SettlementHistoryRequest
import com.cogoport.ares.model.settlement.SettlementKnockoffRequest
import com.cogoport.ares.model.settlement.SettlementKnockoffResponse
import com.cogoport.ares.model.settlement.SettlementRequest
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryRequest
import com.cogoport.ares.model.settlement.SummaryResponse
import com.cogoport.ares.model.settlement.TdsSettlementDocumentRequest
import com.cogoport.ares.model.settlement.TdsStyle
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.transaction.Transactional
import kotlin.math.ceil
import kotlin.math.roundToInt

@Singleton
open class SettlementServiceImpl : SettlementService {

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var historyDocumentConverter: HistoryDocumentMapper

    @Inject
    lateinit var settledInvoiceConverter: SettledInvoiceMapper

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject
    lateinit var documentConverter: DocumentMapper

    @Inject
    lateinit var orgSummaryConverter: OrgSummaryMapper

    /**
     * *
     * - add entry into payments table
     * - add into account utilization table
     * - add entries into settlement table
     * - invoice knocked off with amount
     * - tds entry
     * - payment entry
     * - convenience fee entry [like EXC/TDS] for accounting of payment
     * - update utilization table with balance or status
     */
    override suspend fun knockoff(request: SettlementKnockoffRequest): SettlementKnockoffResponse {

        val invoiceUtilization =
            accountUtilizationRepository.findRecordByDocumentValue(
                documentValue = request.invoiceNumber,
                accType = AccountType.SINV.toString(),
                accMode = AccMode.AR.toString()
            ) ?: throw AresException(AresError.ERR_1002, AresConstants.ZONE)

        val payment = settledInvoiceConverter.convertKnockoffRequestToEntity(request)
        payment.organizationId = invoiceUtilization.organizationId
        payment.organizationName = invoiceUtilization.organizationName
        payment.exchangeRate =
            getExchangeRate(
                payment.currency,
                invoiceUtilization.ledCurrency
            )
        payment.ledCurrency = invoiceUtilization.ledCurrency
        payment.accMode = AccMode.AR
        payment.ledAmount = payment.amount * payment.exchangeRate!!

        // Utilization of payment
        val documentNo = sequenceGeneratorImpl.getPaymentNumber(SequenceSuffix.RECEIVED.prefix)

        val accountUtilization =
            AccountUtilization(
                id = null,
                documentNo = documentNo,
                documentValue = request.transactionId,
                zoneCode = invoiceUtilization.zoneCode,
                serviceType = invoiceUtilization.serviceType,
                documentStatus = DocumentStatus.FINAL,
                entityCode = invoiceUtilization.entityCode,
                category = invoiceUtilization.category,
                orgSerialId = invoiceUtilization.orgSerialId,
                sageOrganizationId = invoiceUtilization.sageOrganizationId,
                organizationId = invoiceUtilization.organizationId,
                organizationName = invoiceUtilization.organizationName,
                accType = AccountType.REC,
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
                transactionDate = request.transactionDate,
                dueDate = request.transactionDate
            )

        val isTdsApplied =
            settlementRepository.countDestinationBySourceType(
                invoiceUtilization.documentNo,
                SettlementType.SINV,
                SettlementType.CTDS
            ) > 0

        val settlements = mutableListOf<Settlement>()
        val amountToSettle = invoiceUtilization.amountCurr - invoiceUtilization.payCurr
        var payAmount = request.amount
        if (invoiceUtilization.currency != request.currency) {
            payAmount =
                payment.amount *
                getExchangeRate(
                    request.currency,
                    invoiceUtilization.currency
                )
        }

        val settledAmount: BigDecimal = if (payAmount > amountToSettle) {
            amountToSettle
        } else {
            payAmount
        }

        settlements.add(
            Settlement(
                id = null,
                sourceId = accountUtilization.documentNo,
                sourceType = SettlementType.REC,
                destinationId = invoiceUtilization.documentNo,
                destinationType = SettlementType.SINV,
                currency = invoiceUtilization.currency,
                amount = settledAmount,
                ledCurrency = invoiceUtilization.ledCurrency,
                ledAmount =
                settledAmount *
                    payment.exchangeRate!!, // getExchangeRate(invoiceUtilization.ledCurrency,invoiceUtilization.currency,payment.transactionDate!!),
                signFlag = 1,
                settlementDate = Timestamp.from(Instant.now()),
                createdAt = Timestamp.from(Instant.now()),
                createdBy = null,
                updatedBy = null,
                updatedAt = Timestamp.from(Instant.now())
            )
        )

        if (!isTdsApplied) {
            val tds: BigDecimal = invoiceUtilization.taxableAmount!!.multiply(0.02.toBigDecimal())
            settlements.add(
                Settlement(
                    id = null,
                    sourceId = accountUtilization.documentNo,
                    sourceType = SettlementType.CTDS,
                    destinationId = invoiceUtilization.documentNo,
                    destinationType = SettlementType.SINV,
                    currency = invoiceUtilization.currency,
                    amount = tds,
                    ledCurrency = invoiceUtilization.ledCurrency,
                    ledAmount =
                    tds *
                        getExchangeRate(
                            invoiceUtilization.ledCurrency,
                            invoiceUtilization.currency
                        ),
                    signFlag = 1,
                    settlementDate = Timestamp.from(Instant.now()),
                    createdAt = Timestamp.from(Instant.now()),
                    createdBy = null,
                    updatedBy = null,
                    updatedAt = Timestamp.from(Instant.now())
                )
            )
        }

        val settledAmtLed =
            settledAmount * invoiceUtilization.amountLoc / invoiceUtilization.amountCurr
        val settledAmtFromRec = settledAmount * payment.exchangeRate!!

        if (settledAmtLed != settledAmtFromRec) {
            settlements.add(
                Settlement(
                    id = null,
                    sourceId = accountUtilization.documentNo,
                    sourceType = SettlementType.SECH,
                    destinationId = invoiceUtilization.documentNo,
                    destinationType = SettlementType.SINV,
                    currency = null,
                    amount = null,
                    ledCurrency = invoiceUtilization.ledCurrency,
                    ledAmount = settledAmtFromRec - settledAmtLed,
                    signFlag = 1,
                    settlementDate = Timestamp.from(Instant.now()),
                    createdAt = Timestamp.from(Instant.now()),
                    createdBy = null,
                    updatedBy = null,
                    updatedAt = Timestamp.from(Instant.now())
                )
            )
        }

        invoiceUtilization.payCurr = invoiceUtilization.payCurr + settledAmount
        invoiceUtilization.payLoc = invoiceUtilization.payCurr * payment.exchangeRate!!
        invoiceUtilization.updatedAt = Timestamp.from(Instant.now())

        accountUtilization.payLoc = settledAmount
        accountUtilization.payCurr = accountUtilization.payCurr * payment.exchangeRate!!

        // 2% tds on taxable amount only if tds is not deducted already
        payment.createdAt = Timestamp.from(Instant.now())
        payment.updatedAt = Timestamp.from(Instant.now())
        paymentRepository.save(payment)
        accountUtilizationRepository.update(invoiceUtilization)
        accountUtilizationRepository.save(accountUtilization)
        settlements.forEach { settlement -> settlementRepository.save(settlement) }
        return SettlementKnockoffResponse()
    }

    /**
     * Get invoices for Given CP orgId
     * @param SettlementDocumentRequest
     * @return ResponseList
     */
    override suspend fun getInvoices(request: SettlementDocumentRequest) = getInvoiceList(request)

    override suspend fun getDocuments(request: SettlementDocumentRequest): ResponseList<Document>? {
        validateSettlementDocumentInput(request)
        return getDocumentList(request)
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
     * @param SummaryRequest
     * @return SummaryResponse
     */
    override suspend fun getAccountBalance(request: SummaryRequest): SummaryResponse {
        val amount =
            accountUtilizationRepository.getAccountBalance(
                request.orgId!!,
                request.entityCode!!,
                request.startDate,
                request.endDate
            )
        return SummaryResponse(amount)
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
        val documents =
            accountUtilizationRepository.getHistoryDocument(
                request.orgId,
                accountTypes,
                request.page,
                request.pageLimit,
                request.startDate,
                request.endDate
            )
        val totalRecords =
            if (request.accountType == "All") {
                accountUtilizationRepository.countHistoryDocument(
                    request.orgId,
                    listOf(AccountType.PCN, AccountType.REC),
                    request.startDate,
                    request.endDate
                )
            } else {
                accountUtilizationRepository.countHistoryDocument(
                    request.orgId,
                    listOf(AccountType.valueOf(request.accountType)),
                    request.startDate,
                    request.endDate
                )
            }
        val historyDocuments = mutableListOf<HistoryDocument>()
        documents.forEach { doc ->
            historyDocuments.add(historyDocumentConverter.convertToModel(doc))
        }
        return ResponseList(
            list = historyDocuments,
            totalPages = getTotalPages(totalRecords, request.pageLimit),
            totalRecords = totalRecords,
            pageNo = request.page
        )
    }

    private fun stringAccountTypes(request: SettlementHistoryRequest): MutableList<String> {
        val accountTypes =
            if (request.accountType == "All") {
                mutableListOf(AccountType.PCN.toString(), AccountType.REC.toString())
            } else {
                mutableListOf(request.accountType)
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
    override suspend fun getSettlement(
        request: SettlementRequest
    ): ResponseList<com.cogoport.ares.model.settlement.SettledInvoice?> {
        val settledDocuments = mutableListOf<com.cogoport.ares.model.settlement.SettledInvoice>()
        var settlements = mutableListOf<SettledInvoice>()
        when (request.settlementType) {
            SettlementType.REC, SettlementType.PCN -> {
                @Suppress("UNCHECKED_CAST")
                settlements =
                    settlementRepository.findSettlement(
                    request.documentNo,
                    request.settlementType,
                    request.page,
                    request.pageLimit
                ) as MutableList<SettledInvoice>
            }
            else -> {}
        }

        val totalRecords =
            settlementRepository.countSettlement(request.documentNo, request.settlementType)

        settlements.forEach { settlement ->
            when (request.settlementType) {
                SettlementType.REC, SettlementType.PCN -> {
                    val settled = settledInvoiceConverter.convertToModel(settlement)
                    when (settled.balanceAmount) {
                        BigDecimal.ZERO -> settled.status = InvoiceStatus.PAID.value
                        settled.documentAmount -> settled.status = InvoiceStatus.UNPAID.value
                        else -> settled.status = InvoiceStatus.PARTIAL_PAID.value
                    }
                    settledDocuments.add(settled)
                }
                else -> {}
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
    private suspend fun getDocumentList(
        request: SettlementDocumentRequest
    ): ResponseList<Document> {
        val offset = (request.pageLimit * request.page) - request.pageLimit
        val documentEntity =
            accountUtilizationRepository.getDocumentList(
                request.pageLimit,
                offset,
                request.accType,
                request.orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                "%${request.query}%"
            )
        val documentModel = documentEntity.map { documentConverter.convertToModel(it!!) }
        val total =
            accountUtilizationRepository.getDocumentCount(
                request.accType,
                request.orgId,
                request.entityCode,
                request.startDate,
                request.endDate,
                "%${request.query}%"
            )
        for (doc in documentModel) {
            doc.documentType = getInvoiceType(AccountType.valueOf(doc.documentType))
            doc.status = getInvoiceStatus(doc.afterTdsAmount, doc.balanceAmount)
            doc.settledAllocation = BigDecimal.ZERO
            doc.settledTds = BigDecimal.ZERO
            doc.allocationAmount = doc.balanceAmount
            doc.balanceAfterAllocation = BigDecimal.ZERO
        }
        return ResponseList(
            list = documentModel,
            totalPages = ceil(total?.toDouble()?.div(request.pageLimit) ?: 0.0).toLong(),
            totalRecords = total,
            pageNo = request.page
        )
    }

    /** Validate input for list of documents */
    private fun validateSettlementDocumentInput(request: SettlementDocumentRequest) {
        if (request.entityCode == null) throw AresException(AresError.ERR_1003, "entityCode")
        if (request.orgId.isEmpty()) throw AresException(AresError.ERR_1003, "orgId")
    }

    /**
     * Get List of Documents from OpenSearch index_account_utilization
     * @param SettlementDocumentRequest
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
        val documentModel = documentEntity.map { documentConverter.convertToModel(it!!) }
        val total =
            accountUtilizationRepository.getTDSDocumentCount(
                request.accType,
                request.orgId,
                request.accMode,
                request.startDate,
                request.endDate,
                "%${request.query}%"
            )
        for (doc in documentModel) {
            doc.documentType = getInvoiceType(AccountType.valueOf(doc.documentType))
            doc.status = getInvoiceStatus(doc.afterTdsAmount, doc.balanceAmount)
        }
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

    /**
     * Get List of invoices for CP.
     * @param SettlementDocumentRequest
     * @return ResponseList
     */
    private suspend fun getInvoiceList(request: SettlementDocumentRequest): ResponseList<Invoice> {
        if (request.orgId.isEmpty()) throw AresException(AresError.ERR_1003, "orgId")
        request.accType = AccountType.SINV
        val response = getDocumentList(request)
        val invoiceList = documentConverter.convertToInvoice(response.list)
        return ResponseList(
            list = invoiceList,
            totalPages = response.totalPages,
            totalRecords = response.totalRecords,
            pageNo = request.page
        )
    }

    override suspend fun check(request: CheckRequest): List<CheckDocument> =
        runSettlement(request, false)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun settle(request: CheckRequest): List<CheckDocument> =
        runSettlement(request, true)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun edit(request: CheckRequest): List<CheckDocument> = editSettlement(request)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun editTds(request: EditTdsRequest) = editInvoiceTds(request)

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun delete(documentNo: Long, settlementType: SettlementType) =
        deleteSettlement(documentNo, settlementType)

    override suspend fun getOrgSummary(
        orgId: UUID,
        startDate: Timestamp?,
        endDate: Timestamp?
    ): OrgSummaryResponse {
        val responseEntity =
            accountUtilizationRepository.getOrgSummary(orgId, startDate, endDate)
                ?: throw AresException(AresError.ERR_1005, "")
        val responseModel = orgSummaryConverter.convertToModel(responseEntity)
        responseModel.tdsStyle = TdsStyle(style = "gross", rate = 2.toBigDecimal())
        return responseModel
    }

    private suspend fun editInvoiceTds(request: EditTdsRequest): Long {
        val doc =
            settlementRepository.findByDestIdAndDestType(
                request.documentNo!!,
                request.settlementType!!
            )
        val tdsDoc =
            doc.first { it?.sourceType in listOf(SettlementType.CTDS, SettlementType.VTDS) }
                ?: throw AresException(AresError.ERR_1503, "TDS")
        val sourceDoc =
            doc.first { it.sourceType in fetchSettlingDocs(it?.destinationType!!) }
                ?: throw AresException(AresError.ERR_1503, "PAYMENT")
        val sourceLedgerRate =
            Utilities.binaryOperation(sourceDoc.ledAmount, sourceDoc.amount!!, Operator.DIVIDE)
        var currNewTds = request.newTds!!
        if (sourceDoc.currency != request.currency) {
            val rate =
                if (sourceDoc.ledCurrency == request.currency) {
                    sourceLedgerRate
                } else {
                    accountUtilizationRepository.findRecord(
                        sourceDoc.sourceId!!,
                        sourceDoc.sourceType.toString()
                    )
                        ?: throw AresException(
                            AresError.ERR_1503,
                            "${sourceDoc.sourceType}_${sourceDoc.sourceId}"
                        )
                    getExchangeRate(
                        sourceDoc.currency!!,
                        request.currency!!
                    )
                }
            currNewTds = getExchangeValue(request.newTds!!, rate, true)
        }
        if (currNewTds > tdsDoc.amount!!) {
            val paymentTdsDiff = currNewTds - tdsDoc.amount!!
            reduceAccountUtilization(
                sourceDoc.sourceId!!,
                AccountType.valueOf(sourceDoc.sourceType.toString()),
                paymentTdsDiff,
                Utilities.binaryOperation(paymentTdsDiff, sourceLedgerRate, Operator.MULTIPLY)
            )
        } else if (currNewTds < tdsDoc.amount) {
            val invoiceTdsDiff = request.oldTds!! - request.newTds!!
            val paymentTdsDiff = tdsDoc.amount!! - currNewTds
            reduceAccountUtilization(
                tdsDoc.destinationId,
                AccountType.valueOf(tdsDoc.destinationType.toString()),
                invoiceTdsDiff,
                Utilities.binaryOperation(
                    invoiceTdsDiff,
                    request.exchangeRate!!,
                    Operator.MULTIPLY
                )
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
            settlementRepository.update(sourceDoc)
        }
        tdsDoc.amount = currNewTds
        tdsDoc.ledAmount =
            Utilities.binaryOperation(currNewTds, sourceLedgerRate, Operator.MULTIPLY)
        settlementRepository.update(tdsDoc)
        return tdsDoc.destinationId
    }

    private suspend fun editSettlement(request: CheckRequest): List<CheckDocument> {
        val sourceDoc =
            request.stackDetails.first {
                it.accountType in listOf(SettlementType.REC, SettlementType.PCN)
            }
        deleteSettlement(sourceDoc.documentNo, sourceDoc.accountType)
        return runSettlement(request, true)
    }

    private suspend fun deleteSettlement(documentNo: Long, settlementType: SettlementType): Long {
        val sourceType =
            if (settlementType == SettlementType.REC)
                listOf(SettlementType.REC, SettlementType.CTDS, SettlementType.SECH)
            else listOf(SettlementType.PCN, SettlementType.VTDS, SettlementType.PECH)
        val fetchedDoc = settlementRepository.findBySourceIdAndSourceType(documentNo, sourceType)
        val debitDoc = fetchedDoc.groupBy { it?.destinationId }
        val sourceCurr =
            fetchedDoc.sumOf {
                it?.amount?.multiply(BigDecimal.valueOf(it.signFlag.toLong()))
                    ?: BigDecimal.ZERO
            }
        reduceAccountUtilization(
            documentNo,
            AccountType.valueOf(settlementType.toString()),
            sourceCurr
        )
        for (debit in debitDoc) {
            val settledDoc =
                debit.value.first { it?.sourceType == settlementType }
                    ?: throw AresException(AresError.ERR_1501, "")
            val payment =
                accountUtilizationRepository.findRecord(
                    settledDoc.sourceId!!,
                    settledDoc.sourceType.toString()
                )
                    ?: throw AresException(
                        AresError.ERR_1503,
                        settledDoc.sourceId.toString()
                    )
            val invoice =
                accountUtilizationRepository.findRecord(
                    settledDoc.destinationId,
                    settledDoc.destinationType.toString()
                )
                    ?: throw AresException(
                        AresError.ERR_1503,
                        settledDoc.destinationId.toString()
                    )
            var settledCurr = settledDoc.amount!!
            if (payment.currency != invoice.currency) {
                val rate =
                    if (payment.ledCurrency == invoice.currency) {
                        Utilities.binaryOperation(
                            payment.amountLoc,
                            payment.amountCurr,
                            Operator.DIVIDE
                        )
                    } else {
                        getExchangeRate(
                            payment.currency,
                            invoice.currency
                        )
                    }
                settledCurr = getExchangeValue(settledCurr, rate)
            }
            reduceAccountUtilization(
                debit.key!!,
                AccountType.valueOf(settledDoc.destinationType.toString()),
                settledCurr
            )
        }
        settlementRepository.deleteByIdIn(fetchedDoc.map { it?.id!! })
        return documentNo
    }

    private suspend fun reduceAccountUtilization(
        docId: Long,
        accType: AccountType,
        amount: BigDecimal,
        ledAmount: BigDecimal? = null
    ) {
        val accUtil =
            accountUtilizationRepository.findRecord(docId, accType.toString())
                ?: throw AresException(AresError.ERR_1503, "${accType}_$docId")
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
        accountUtilizationRepository.update(accUtil)
    }

    private suspend fun runSettlement(
        request: CheckRequest,
        performDbOperation: Boolean
    ): List<CheckDocument> {
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
                SettlementType.PDN
            )
        for (doc in request.stackDetails.reversed()) {
            if (creditType.contains(doc.accountType)) {
                source.add(doc)
            } else if (debitType.contains(doc.accountType)) {
                dest.add(doc)
            }
        }
        if (source.isEmpty() &&
            dest.map { it.accountType }.contains(SettlementType.SINV) &&
            dest.map { it.accountType }.contains(SettlementType.PINV)
        ) {
            dest.filter { it.accountType == SettlementType.SINV }.forEach {
                source.add(it)
                dest.remove(it)
            }
        }
        businessValidation(source, dest)
        val settledList = settleDocuments(request, source, dest, performDbOperation)
        return request.stackDetails.map { r -> settledList.filter { it.id == r.id }[0] }
    }

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
                if (canSettle.contains(invoice.accountType) &&
                    availableAmount.compareTo(0.toBigDecimal()) != 0
                ) {
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
            }
            payment.allocationAmount -= availableAmount
            payment.balanceAfterAllocation =
                payment.balanceAmount.subtract(payment.allocationAmount)
            assignStatus(payment)
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
        if (toSettleAmount != 0.0.toBigDecimal()) {
            var rate = 1.toBigDecimal()
            val ledgerRate = payment.exchangeRate
            var updateDoc = true
            if (payment.currency != invoice.currency) {
                rate =
                    if (payment.ledCurrency == invoice.currency) {
                        ledgerRate
                    } else {
                        getExchangeRate(
                            payment.currency,
                            invoice.currency
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
                        updateDoc,
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
        val invoiceTds = invoice.tds!! - invoice.settledTds
        var paymentTds = getExchangeValue(invoiceTds, exchangeRate, true)
        if (payment.accountType !in (listOf(SettlementType.PCN, SettlementType.SCN))) {
            paymentTds = 0.toBigDecimal()
        }
        val amount = availableAmount - toSettleAmount - paymentTds
        invoice.settledAllocation += toSettleAmount
        payment.settledAllocation +=
            getExchangeValue(toSettleAmount + paymentTds, exchangeRate, true)
        if (updateDoc) {
            invoice.allocationAmount = invoice.settledAllocation
            invoice.balanceAfterAllocation =
                invoice.balanceAmount.subtract(invoice.allocationAmount)
        }
        assignStatus(invoice)
        assignStatus(payment)
        if (performDbOperation)
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
        val paidAmount = getExchangeValue(toSettleAmount, exchangeRate, true)
        val paidLedAmount = getExchangeValue(paidAmount, ledgerRate)
        val invoiceTds = invoice.tds!! - invoice.settledTds
        val paymentTds = getExchangeValue(invoiceTds, exchangeRate, true)
        val paymentTdsLed = getExchangeValue(paymentTds, ledgerRate)
        createSettlement(
            payment.documentNo,
            payment.accountType,
            invoice,
            payment.currency,
            (paidAmount + paymentTds),
            payment.ledCurrency,
            (paidLedAmount + paymentTdsLed),
            1,
            request.settlementDate
        )
        if (paymentTds.compareTo(0.toBigDecimal()) != 0) {
            val tdsType =
                if (fetchSettlingDocs(SettlementType.CTDS).contains(invoice.accountType))
                    SettlementType.CTDS
                else SettlementType.VTDS
            createSettlement(
                payment.documentNo,
                tdsType,
                invoice,
                payment.currency,
                paymentTds,
                invoice.ledCurrency,
                paymentTdsLed,
                -1,
                request.settlementDate
            )
            invoice.settledTds += invoiceTds
        }
        if (payment.ledCurrency != invoice.currency) {
            val excLedAmount =
                getExchangeValue(toSettleAmount, invoice.exchangeRate) - (paidLedAmount)
            val exType =
                if (fetchSettlingDocs(SettlementType.CTDS).contains(invoice.accountType))
                    SettlementType.SECH
                else SettlementType.PECH
            val exSign =
                excLedAmount.signum() *
                    if (payment.accountType in
                        listOf(
                                SettlementType.SCN,
                                SettlementType.REC,
                                SettlementType.SINV
                            )
                    )
                        -1
                    else 1
            createSettlement(
                payment.documentNo,
                exType,
                invoice,
                null,
                null,
                invoice.ledCurrency,
                excLedAmount.abs(),
                exSign.toShort(),
                request.settlementDate
            )
        }
        val paymentUtilized =
            paidAmount +
                if (payment.accountType in listOf(SettlementType.PCN, SettlementType.SCN))
                    paymentTds
                else 0.toBigDecimal()
        updateAccountUtilization(payment, paymentUtilized)
        updateAccountUtilization(invoice, (toSettleAmount + invoiceTds))
    }

    private suspend fun updateAccountUtilization(
        document: CheckDocument,
        utilizedAmount: BigDecimal
    ) {
        val paymentUtilization =
            accountUtilizationRepository.findRecord(
                document.documentNo,
                document.accountType.toString()
            )
                ?: throw AresException(
                    AresError.ERR_1503,
                    "${document.documentNo}_${document.accountType}"
                )
        paymentUtilization.payCurr += utilizedAmount
        paymentUtilization.payLoc += getExchangeValue(utilizedAmount, document.exchangeRate)
        accountUtilizationRepository.update(paymentUtilization)
    }

    private suspend fun createSettlement(
        sourceId: Long?,
        sourceType: SettlementType,
        invoice: CheckDocument,
        currency: String?,
        amount: BigDecimal?,
        ledCurrency: String,
        ledAmount: BigDecimal,
        signFlag: Short,
        transactionDate: Timestamp
    ) {
        val settledDoc =
            Settlement(
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

    private fun getExchangeRate(from: String, to: String): BigDecimal {
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
            SettlementType.REC -> {
                listOf(SettlementType.SINV, SettlementType.SDN)
            }
            SettlementType.PINV -> {
                listOf(SettlementType.PAY, SettlementType.PCN, SettlementType.SINV)
            }
            SettlementType.PCN -> {
                listOf(SettlementType.PINV, SettlementType.PDN)
            }
            SettlementType.PAY -> {
                listOf(SettlementType.PINV, SettlementType.PDN)
            }
            SettlementType.SINV -> {
                listOf(SettlementType.REC, SettlementType.SCN, SettlementType.PINV)
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
                listOf(SettlementType.SINV, SettlementType.SDN)
            }
            SettlementType.VTDS -> {
                listOf(SettlementType.PINV, SettlementType.PDN)
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun sanitizeInput(request: CheckRequest) {
        for (doc in request.stackDetails) {
            if (doc.documentNo == 0.toLong())
                throw AresException(AresError.ERR_1003, "Document Number")
        }
        request.stackDetails.forEach { it.settledAllocation = BigDecimal.ZERO }
    }

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

    private fun assignStatus(doc: CheckDocument) {
        if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) == 0) {
            doc.status = InvoiceStatus.KNOCKED_OFF.value
        } else if (decimalRound(doc.settledAllocation).compareTo(0.toBigDecimal()) == 0) {
            doc.status = InvoiceStatus.UNPAID.value
        } else if (decimalRound(doc.balanceAmount).compareTo(decimalRound(doc.settledAllocation)) ==
            1
        ) {
            doc.status = InvoiceStatus.PARTIAL_PAID.value
        }
    }

    private fun decimalRound(amount: BigDecimal): BigDecimal {
        return Utilities.decimalRound(amount)
    }

    private fun getInvoiceStatus(afterTdsAmount: BigDecimal, balanceAmount: BigDecimal): String {
        return if (balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
            InvoiceStatus.PAID.value
        } else if (afterTdsAmount.compareTo(balanceAmount) != 0) {
            InvoiceStatus.PARTIAL_PAID.value
        } else {
            InvoiceStatus.UNPAID.value
        }
    }

    private fun getInvoiceType(accType: AccountType): String {
        return when (accType) {
            AccountType.SINV -> {
                InvoiceType.SINV.value
            }
            AccountType.SCN -> {
                InvoiceType.SCN.value
            }
            AccountType.SDN -> {
                InvoiceType.SDN.value
            }
            AccountType.REC -> {
                InvoiceType.REC.value
            }
            AccountType.PINV -> {
                InvoiceType.PINV.value
            }
            AccountType.PCN -> {
                InvoiceType.PCN.value
            }
            AccountType.PDN -> {
                InvoiceType.PDN.value
            }
            AccountType.PAY -> {
                InvoiceType.PAY.value
            }
        }
    }
}
