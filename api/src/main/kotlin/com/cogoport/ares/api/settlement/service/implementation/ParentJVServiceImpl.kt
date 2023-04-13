package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.ParentJVRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.settlement.service.interfaces.ParentJVService
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.sage.SageCustomerRecord
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.JVSageControls
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentJVUpdateRequest
import com.cogoport.ares.model.settlement.request.ParentJournalVoucherRequest
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.sage.Client
import com.cogoport.brahma.sage.SageException
import com.cogoport.brahma.sage.model.request.JVLineItem
import com.cogoport.brahma.sage.model.request.JVRequest
import com.cogoport.brahma.sage.model.request.SageResponse
import com.cogoport.hades.client.HadesClient
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import com.cogoport.plutus.model.invoice.SageOrganizationResponse
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.json.JSONObject
import org.json.XML
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.transaction.Transactional

@Singleton
open class ParentJVServiceImpl : ParentJVService {

    @Inject
    lateinit var parentJVRepository: ParentJVRepository

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject
    lateinit var journalVoucherConverter: JournalVoucherMapper

    @Inject
    lateinit var journalVoucherService: JournalVoucherService

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var hadesClient: HadesClient

    @Inject
    lateinit var railsClient: RailsClient

    @Inject
    lateinit var authClient: AuthClient

    @Inject
    lateinit var auditService: AuditService

    @Inject
    lateinit var thirdPartyApiAuditService: ThirdPartyApiAuditService

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    /**
     * Create a journal voucher and add it to account_utilizationns.
     * @param: ParentJournalVoucherRequest
     * @return: Parent JV Id
     */

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun createJournalVoucher(request: ParentJournalVoucherRequest): String {
        validateCreateRequest(request)

        val parentJvNum = getJvNumber()
        val transactionDate = request.transactionDate

        val parentData = ParentJournalVoucher(
            id = null,
            status = JVStatus.PENDING,
            category = request.jvCategory,
            validityDate = transactionDate,
            jvNum = parentJvNum,
            createdBy = request.createdBy,
            updatedBy = request.createdBy,
            currency = request.currency,
            description = request.description,
            exchangeRate = request.exchangeRate,
            jvCodeNum = request.jvCodeNum,
            ledCurrency = request.ledCurrency,
            entityCode = request.entityCode,
            transactionDate = request.transactionDate
        )
        val parentJvData = parentJVRepository.save(parentData)

        creatingLineItemsAndRequestToIncident(request, parentJvData, transactionDate)

        return Hashids.encode(parentJvData.id!!)
    }

    /**
     * Get List of JVs based on input filters.
     * @param: jvListRequest
     * @return: ResponseList
     */

    override suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<ParentJournalVoucherResponse> {
        val documentEntity = parentJVRepository.getListVouchers(
            jvListRequest.status,
            if (jvListRequest.category != null) jvListRequest.category!! else null,
            jvListRequest.query,
            jvListRequest.page,
            jvListRequest.pageLimit,
            jvListRequest.sortType,
            jvListRequest.sortBy
        )
        val totalRecords =
            parentJVRepository.countDocument(
                jvListRequest.status,
                if (jvListRequest.category != null) jvListRequest.category!! else null,
                jvListRequest.query
            )

        val jvList = mutableListOf<ParentJournalVoucherResponse>()
        documentEntity.forEach { doc ->
            val jvData = journalVoucherConverter.convertICJVEntityToModel((doc))
            jvData.id = Hashids.encode(jvData.id.toLong())
            jvList.add(jvData)
        }

        return ResponseList(
            list = jvList,
            totalPages = Utilities.getTotalPages(totalRecords, jvListRequest.pageLimit),
            totalRecords = totalRecords,
            pageNo = jvListRequest.page
        )
    }

    private fun validateCreateRequest(request: ParentJournalVoucherRequest) {
        if (request.createdBy == null) throw AresException(AresError.ERR_1003, "Created By")

        if (request.jvLineItems.any { it.glCode == null }) throw AresException(AresError.ERR_1003, "GL Code")

        if (request.jvLineItems.groupBy { it.entityCode }.values.first().filter { it.type == "DEBIT" }.sumOf { it.amount } != request.jvLineItems.groupBy { it.entityCode }.values.first().filter { it.type == "CREDIT" }.sumOf { it.amount }) {
            throw AresException(AresError.ERR_1527, "")
        }
    }

    private suspend fun getJvNumber() =
        SequenceSuffix.JV.prefix + "/" + Utilities.getFinancialYear() + "/" + sequenceGeneratorImpl.getPaymentNumber(
            SequenceSuffix.JV.prefix
        )

    private fun getSignFlag(type: String): Short {
        return when (type.uppercase()) {
            "CREDIT" -> { -1 }
            "DEBIT" -> { 1 }
            else -> {
                throw AresException(AresError.ERR_1009, "JV type")
            }
        }
    }

    /**
     * Update JV based on input filters.
     * @param: ParentJVUpdateRequest
     * @return: Updated Jv I'd
     */

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun updateParentJv(request: ParentJVUpdateRequest): String {
        val parentJvId = Hashids.decode(request.parentJvId!!)[0]

        // Update Parent Journal Voucher
        val parentJvData = parentJVRepository.findById(parentJvId) ?: throw AresException(AresError.ERR_1519, "")

        when (parentJvData.status == request.status!!) {
            true -> throw AresException(AresError.ERR_1520, "${request.status}")
            false -> parentJvData.status = request.status!!
        }

        parentJVRepository.update(parentJvData)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PARENT_JOURNAL_VOUCHERS,
                objectId = parentJvId,
                actionName = AresConstants.UPDATE,
                data = parentJvData,
                performedBy = request.performedBy.toString(),
                performedByUserType = null
            )
        )

        val jvLineItemData = journalVoucherRepository.getJournalVoucherByParentJVId(parentJvId)

        jvLineItemData.map {
            // Update Journal Voucher Line Items
            it.status = request.status!!
            it.updatedAt = Timestamp.from(Instant.now())
            it.updatedBy = request.performedBy
            it.description = request.remark
            journalVoucherRepository.update(it)

            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.JOURNAL_VOUCHERS,
                    objectId = it.id,
                    actionName = AresConstants.UPDATE,
                    data = it,
                    performedBy = request.performedBy.toString(),
                    performedByUserType = null
                )
            )

            // Insert JV Line Items in account_utilizations
            if (it.accMode != AccMode.OTHER) {
                val accMode = AccMode.valueOf(it.accMode!!.name)
                val signFlag = getSignFlag(it.type!!)

                if (it.tradePartyId != null) {
                    journalVoucherService.createJvAccUtil(it, accMode, signFlag)
                }
            }
        }

        return request.parentJvId!!
    }

    /**
     * Delete JV based on id.
     * @param: I'd and performed by
     * @return: Deleted Jv Id
     */

    override suspend fun deleteJournalVoucherById(id: String, performedBy: UUID): String {
        val parentJvId = Hashids.decode(id)[0]
        parentJVRepository.deleteJournalVoucherById(parentJvId, performedBy)
        val jvLineItemData = journalVoucherRepository.getJournalVoucherByParentJVId(parentJvId)
        jvLineItemData.forEach { lineItem ->
            if (lineItem.status == JVStatus.APPROVED) {
                accountUtilizationRepository.deleteAccountUtilizationByDocumentValueAndAccType(lineItem.jvNum, AccountType.valueOf(lineItem.category))
            }
        }
        journalVoucherRepository.deleteJvLineItemByParentJvId(parentJvId, performedBy)

        return id
    }

    /**
     * Edit JV line item based on input filters.
     * @param: ParentJournalVoucherRequest
     * @return: Edited Jv I'd
     */

    override suspend fun editJv(request: ParentJournalVoucherRequest): String {
        validateCreateRequest(request)

        val parentJvNum = getJvNumber()
        val transactionDate = request.transactionDate

        val parentData = ParentJournalVoucher(
            id = Hashids.decode(request.id!!)[0],
            status = JVStatus.PENDING,
            category = request.jvCategory,
            validityDate = transactionDate,
            jvNum = parentJvNum,
            createdBy = request.createdBy,
            updatedBy = request.createdBy,
            currency = request.currency,
            description = request.description,
            exchangeRate = request.exchangeRate,
            jvCodeNum = request.jvCodeNum,
            ledCurrency = request.ledCurrency,
            entityCode = request.entityCode,
            transactionDate = request.transactionDate
        )

        val parentJvData = parentJVRepository.update(parentData)

        // deleting all line items with parentId
        journalVoucherRepository.deletingLineItemsWithParentJvId(parentJvData.id!!)
        accountUtilizationRepository.deleteAccountUtilizationByDocumentValueAndAccType(parentJvData.jvNum, AccountType.valueOf(parentJvData.category))

        creatingLineItemsAndRequestToIncident(request, parentJvData, transactionDate)

        return Hashids.encode(parentJvData.id!!)
    }

    private suspend fun creatingLineItemsAndRequestToIncident(request: ParentJournalVoucherRequest, parentJvData: ParentJournalVoucher?, transactionDate: Date?) {
        request.jvLineItems.forEach { lineItem ->
            if (lineItem.tradePartyName == null && lineItem.tradePartyId != null) {
                val data = railsClient.getListOrganizationTradePartyDetails(lineItem.tradePartyId!!)
                if (data.list.isNotEmpty()) {
                    lineItem.tradePartyName = data.list[0]["legal_business_name"].toString()
                }
            }

            val jvLineItemData = JournalVoucher(
                id = null,
                jvNum = parentJvData?.jvNum!!,
                accMode = lineItem.accMode,
                category = request.jvCategory,
                createdAt = request.createdAt,
                createdBy = request.createdBy,
                updatedAt = request.createdAt,
                updatedBy = request.createdBy,
                currency = request.currency,
                ledCurrency = request.ledCurrency,
                amount = lineItem.amount.multiply(request.exchangeRate),
                ledAmount = lineItem.amount,
                description = request.description,
                entityCode = lineItem.entityCode,
                entityId = UUID.fromString(AresConstants.ENTITY_ID[lineItem.entityCode]),
                exchangeRate = request.exchangeRate,
                glCode = lineItem.glCode,
                parentJvId = parentJvData.id,
                type = lineItem.type,
                signFlag = getSignFlag(lineItem.type),
                status = JVStatus.PENDING,
                tradePartyId = lineItem.tradePartyId,
                tradePartyName = lineItem.tradePartyName,
                validityDate = transactionDate,
                migrated = false,
                deletedAt = null
            )

            journalVoucherRepository.save(jvLineItemData)
        }
    }

    /**
     * Post JV to Sage based on I'd.
     * @param: I'd and Performed By
     * @return: Boolean
     */

    override suspend fun postJVToSage(parentJVId: Long, performedBy: UUID): Boolean {
        try {
            val parentJVDetails = parentJVRepository.findById(parentJVId) ?: throw AresException(AresError.ERR_1002, "")
            val jvLineItems = journalVoucherRepository.getJournalVoucherByParentJVId(parentJVId)
            var sageOrganization: SageOrganizationResponse?

            val jvLineItemsDetails = arrayListOf<JVLineItem>()

            when (parentJVDetails.status) {
                JVStatus.PENDING -> throw AresException(AresError.ERR_1528, "")
                JVStatus.POSTED -> throw AresException(AresError.ERR_1518, "")
                else -> {}
            }

            jvLineItems.map { lineItem ->
                if (lineItem.tradePartyId != null) {
                    val organization = railsClient.getListOrganizationTradePartyDetails(lineItem.tradePartyId)

                    sageOrganization = authClient.getSageOrganization(
                        SageOrganizationRequest(
                            organization.list[0]["serial_id"]!!.toString(),
                            if (lineItem.accMode == AccMode.AP) "service_provider" else "importer_exporter"
                        )
                    )

                    val sageOrganizationQuery = "Select BPCNUM_0 from $sageDatabase.BPCUSTOMER where XX1P4PANNO_0='${organization.list[0]["registration_number"]}'"
                    val resultFromSageOrganizationQuery = Client.sqlQuery(sageOrganizationQuery)
                    val recordsForSageOrganization = ObjectMapper().readValue(resultFromSageOrganizationQuery, SageCustomerRecord::class.java)
                    val sageOrganizationFromSageId = recordsForSageOrganization.recordSet?.get(0)?.sageOrganizationId

                    if (sageOrganization?.sageOrganizationId.isNullOrEmpty()) {
                        parentJVRepository.updateStatus(parentJVId, JVStatus.POSTING_FAILED, performedBy)
                        journalVoucherRepository.updateStatus(lineItem.id!!, JVStatus.POSTING_FAILED, performedBy)

                        thirdPartyApiAuditService.createAudit(
                            ThirdPartyApiAudit(
                                null,
                                "PostJVToSage",
                                "Journal Voucher",
                                parentJVId,
                                "JOURNAL_VOUCHER",
                                "500",
                                sageOrganization.toString(),
                                "Sage organization not present",
                                false
                            )
                        )
                        return false
                    }

                    if (sageOrganization?.sageOrganizationId != sageOrganizationFromSageId) {
                        parentJVRepository.updateStatus(parentJVId, JVStatus.POSTING_FAILED, performedBy)
                        journalVoucherRepository.updateStatus(lineItem.id!!, JVStatus.POSTING_FAILED, performedBy)

                        thirdPartyApiAuditService.createAudit(
                            ThirdPartyApiAudit(
                                null,
                                "PostJVToSage",
                                "Journal Voucher",
                                parentJVId,
                                "JOURNAL_VOUCHER",
                                "500",
                                sageOrganization.toString(),
                                "Sage organization not present",
                                false
                            )
                        )
                        return false
                    }
                } else {
                    sageOrganization = null
                }
                val jvLineItemDetails = getJvLineItem(lineItem, sageOrganization?.sageOrganizationId)
                jvLineItemsDetails.add(jvLineItemDetails)
            }

            lateinit var result: SageResponse

            val destinationDocumentValue = settlementRepository.findBySourceIdAndSourceType(parentJVId, listOf(SettlementType.valueOf(parentJVDetails.category)))

            val mapDestinationDocumentValue = destinationDocumentValue.map {
                it?.destinationId
            }.joinToString(",")

            result = Client.postJVToSage(
                JVRequest
                (
                    parentJVDetails.category,
                    parentJVDetails.jvNum!!,
                    parentJVDetails.entityCode.toString(),
                    parentJVDetails.jvCodeNum!!,
                    parentJVDetails.currency!!,
                    mapDestinationDocumentValue,
                    parentJVDetails.createdAt!!,
                    parentJVDetails.description!!,
                    jvLineItemsDetails
                )
            )

            val processedResponse = XML.toJSONObject(result.response)
            val status = getStatus(processedResponse)

            if (status == 1) {
                parentJVRepository.updateStatus(parentJVId, JVStatus.POSTED, performedBy)
                journalVoucherRepository.updateStatus(parentJVId, JVStatus.POSTED, performedBy)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostJVToSage",
                        "Journal Voucher",
                        parentJVId,
                        "JOURNAL_VOUCHER",
                        "200",
                        result.requestString,
                        result.response,
                        true
                    )
                )
                return true
            } else {
                parentJVRepository.updateStatus(parentJVId, JVStatus.POSTING_FAILED, performedBy)
                journalVoucherRepository.updateStatus(parentJVId, JVStatus.POSTING_FAILED, performedBy)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostJVToSage",
                        "Journal Voucher",
                        parentJVId,
                        "JOURNAL_VOUCHER",
                        "500",
                        result.requestString,
                        result.response,
                        false
                    )
                )
            }
        } catch (exception: SageException) {
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostJVToSage",
                    "Journal Voucher",
                    parentJVId,
                    "JOURNAL_VOUCHER",
                    "500",
                    exception.data,
                    exception.context,
                    false
                )
            )
            throw exception
        } catch (e: Exception) {
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostJVToSage",
                    "Journal Voucher",
                    parentJVId,
                    "JOURNAL_VOUCHER",
                    "500",
                    "",
                    e.toString(),
                    false
                )
            )
            throw e
        }
        return false
    }

    private fun getJvLineItem(journalVoucher: JournalVoucher, sageOrganizationId: String?): JVLineItem {
        return JVLineItem(
            acc = journalVoucher.glCode!!,
            accMode = if (journalVoucher.accMode?.name == null || journalVoucher.accMode!!.name == "") "" else getAccModeValue(journalVoucher.accMode!!),
            sageBPRNumber = if (sageOrganizationId.isNullOrEmpty()) "" else sageOrganizationId,
            description = if (journalVoucher.description.isNullOrEmpty()) "" else journalVoucher.description!!,
            signFlag = getSignFlag(journalVoucher.type.toString().uppercase()).toInt(),
            amount = journalVoucher.amount!!,
            currency = journalVoucher.currency!!
        )
    }

    private fun getAccModeValue(accMode: AccMode): String {
        val accMode = when (accMode) {
            AccMode.AP -> JVSageControls.AP.value
            AccMode.AR -> JVSageControls.AR.value
            AccMode.PDA -> JVSageControls.PDA.value
            AccMode.CSD -> JVSageControls.CSD.value
            AccMode.EMD -> JVSageControls.EMD.value
            AccMode.SUSA -> JVSageControls.SUSA.value
            AccMode.SUSS -> JVSageControls.SUSS.value
            AccMode.OTHER -> JVSageControls.OTHER.value
            else -> {
                throw AresException(AresError.ERR_1529, accMode.name)
            }
        }
        return accMode
    }

    private fun getStatus(processedResponse: JSONObject?): Int? {
        val status = processedResponse?.getJSONObject("soapenv:Envelope")
            ?.getJSONObject("soapenv:Body")
            ?.getJSONObject("wss:runResponse")
            ?.getJSONObject("runReturn")
            ?.getJSONObject("status")
            ?.get("content")
        return status as Int?
    }
}
