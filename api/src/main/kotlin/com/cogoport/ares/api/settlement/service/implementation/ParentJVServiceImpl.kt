package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.enums.ThirdPartyApiNames
import com.cogoport.ares.api.common.enums.ThirdPartyApiType
import com.cogoport.ares.api.common.enums.ThirdPartyObjectName
import com.cogoport.ares.api.common.enums.ThirdPartyResponseCode
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.GlCode
import com.cogoport.ares.api.settlement.entity.GlCodeMaster
import com.cogoport.ares.api.settlement.entity.JournalCode
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.JvCategory
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.repository.GlCodeMasterRepository
import com.cogoport.ares.api.settlement.repository.GlCodeRepository
import com.cogoport.ares.api.settlement.repository.JournalCodeRepository
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.JvCategoryRepository
import com.cogoport.ares.api.settlement.repository.ParentJVRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.settlement.service.interfaces.ParentJVService
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.sage.SageCustomerRecord
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.PostJVToSageRequest
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
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import com.cogoport.plutus.model.invoice.SageOrganizationResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micronaut.context.annotation.Value
import io.sentry.Sentry
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.json.JSONObject
import org.json.XML
import java.math.BigDecimal
import java.math.RoundingMode
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
    lateinit var jvCategoryRepository: JvCategoryRepository

    @Inject
    lateinit var glCodeRepository: GlCodeRepository

    @Inject
    lateinit var glCodeMasterRepository: GlCodeMasterRepository

    @Inject
    lateinit var journalCodeRepository: JournalCodeRepository

    @Inject
    lateinit var railsClient: RailsClient

    @Inject
    lateinit var authClient: AuthClient

    @Inject
    lateinit var auditService: AuditService

    @Inject
    lateinit var thirdPartyApiAuditService: ThirdPartyApiAuditService

    @Inject
    lateinit var aresMessagePublisher: AresMessagePublisher

    @Inject
    lateinit var util: Util

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    /**
     * Create a journal voucher and add it to account_utilizationns.
     * @param: ParentJournalVoucherRequest
     * @return: Parent JV Id
     */

    @Transactional
    override suspend fun createJournalVoucher(request: ParentJournalVoucherRequest): String? {
        validateCreateRequest(request)

        val parentJvNum = getJvNumber()
        val transactionDate = request.transactionDate

        val parentData = ParentJournalVoucher(
            id = null,
            status = JVStatus.APPROVED,
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
            transactionDate = request.transactionDate,
            isUtilized = false
        )
        val parentJvData = parentJVRepository.save(parentData)

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PARENT_JOURNAL_VOUCHERS,
                objectId = parentJvData.id,
                actionName = AresConstants.CREATE,
                data = parentJvData,
                performedBy = request.createdBy.toString(),
                performedByUserType = null
            )
        )

        creatingLineItemsAndRequestToIncident(request, parentJvData, transactionDate)

        if (parentJvData.entityCode != 501) {
            aresMessagePublisher.emitPostJvToSage(
                PostJVToSageRequest(
                    parentJvId = Hashids.encode(parentJvData.id!!),
                    performedBy = parentJvData.createdBy!!

                )
            )
        }

        return parentJvData.jvNum
    }

    /**
     * Get List of JVs based on input filters.
     * @param: jvListRequest
     * @return: ResponseList
     */

    override suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<ParentJournalVoucherResponse> {
        val query = util.toQueryString(jvListRequest.query)
        val sortType = jvListRequest.sortType ?: "Desc"
        val sortBy = jvListRequest.sortBy ?: "createdAt"

        val entityCodes = when (jvListRequest.entityCode != null) {
            true -> when (jvListRequest.entityCode) {
                AresConstants.ENTITY_101 -> listOf(AresConstants.ENTITY_101, AresConstants.ENTITY_201, AresConstants.ENTITY_301, AresConstants.ENTITY_401)
                else -> listOf(jvListRequest.entityCode)
            }
            else -> null
        }

        val documentEntity = parentJVRepository.getListVouchers(
            jvListRequest.status,
            if (jvListRequest.category != null) jvListRequest.category!! else null,
            query,
            jvListRequest.page,
            entityCodes,
            jvListRequest.pageLimit,
            sortType,
            sortBy
        )
        val totalRecords =
            parentJVRepository.countDocument(
                jvListRequest.status,
                if (jvListRequest.category != null) jvListRequest.category!! else null,
                query,
                entityCodes
            )

        val jvList = mutableListOf<ParentJournalVoucherResponse>()
        documentEntity.forEach { doc ->
            val jvData = journalVoucherConverter.convertParentJvEntityToModel((doc))
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

        if (request.jvLineItems.filter { it.type == "DEBIT" }.sumOf { it.amount } != request.jvLineItems.filter { it.type == "CREDIT" }.sumOf { it.amount }) {
            throw AresException(AresError.ERR_1527, "")
        }
    }

    private suspend fun getJvNumber() =
        SequenceSuffix.JV.prefix + "/" + Utilities.getFinancialYear() + "/" + sequenceGeneratorImpl.getPaymentNumber(
            SequenceSuffix.JV.prefix
        )

    private fun getSignFlag(type: String): Short {
        return when (type.uppercase()) {
            "CREDIT" -> -1
            "DEBIT" -> 1
            else -> throw AresException(AresError.ERR_1009, "JV type")
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
                    journalVoucherService.createJvAccUtil(it, accMode, signFlag, true)
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
        try {
            val parentJvDetails = parentJVRepository.findById(parentJvId) ?: throw AresException(AresError.ERR_1002, "JV")
            if (parentJvDetails.isUtilized == true) {
                throw AresException(AresError.ERR_1540, "JV is already utilized.")
            }
            if (parentJvDetails.status == JVStatus.POSTED) {
                val isDeletedFromSage = deleteJvFromSage(parentJvId, parentJvDetails.jvNum!!)
                if (!isDeletedFromSage) {
                    throw AresException(AresError.ERR_1540, "${parentJvDetails.jvNum!!} could not get deleted from sage")
                }
            }
            parentJVRepository.deleteJournalVoucherById(parentJvId, performedBy)
            accountUtilizationRepository.deleteAccountUtilizationByDocumentValueAndAccType(parentJvDetails.jvNum, AccountType.valueOf(parentJvDetails.category))
            journalVoucherRepository.deleteJvLineItemByParentJvId(parentJvId, performedBy)

            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.PARENT_JOURNAL_VOUCHERS,
                    objectId = parentJvId,
                    actionName = AresConstants.DELETE,
                    data = mapOf("id" to id, "status" to "DELETED"),
                    performedBy = performedBy.toString(),
                    performedByUserType = null
                )
            )
        } catch (aresException: AresException) {
            logger().error("""${mapOf("data" to id, "error" to "${aresException.error.message} ${aresException.context} ")}""")
            throw aresException
        } catch (ex: Exception) {
            logger().error(ex.stackTraceToString())
            Sentry.captureException(ex)
            throw ex
        }
        return id
    }

    /**
     * Edit JV line item based on input filters.
     * @param: ParentJournalVoucherRequest
     * @return: Edited Jv I'd
     */

    override suspend fun editJv(request: ParentJournalVoucherRequest): String {
        if (request.id.isNullOrEmpty()) {
            throw AresException(AresError.ERR_1003, "Parent JV Id")
        }
        validateCreateRequest(request)
        val parentJvData = parentJVRepository.findById(Hashids.decode(request.id!!)[0])
        val transactionDate = request.transactionDate

        parentJvData?.status = JVStatus.APPROVED
        parentJvData?.category = request.jvCategory
        parentJvData?.validityDate = transactionDate
        parentJvData?.jvNum = parentJvData?.jvNum
        parentJvData?.createdBy = request.createdBy
        parentJvData?.updatedBy = request.createdBy
        parentJvData?.currency = request.currency
        parentJvData?.description = request.description
        parentJvData?.exchangeRate = request.exchangeRate
        parentJvData?.jvCodeNum = request.jvCodeNum
        parentJvData?.ledCurrency = request.ledCurrency
        parentJvData?.entityCode = request.entityCode
        parentJvData?.transactionDate = request.transactionDate

        val updatedParentJvData = parentJVRepository.update(parentJvData!!)

        // deleting all line items with parentId
        journalVoucherRepository.deletingLineItemsWithParentJvId(updatedParentJvData.id!!)
        accountUtilizationRepository.deleteAccountUtilizationByDocumentValueAndAccType(updatedParentJvData.jvNum, AccountType.valueOf(parentJvData.category))

        creatingLineItemsAndRequestToIncident(request, updatedParentJvData, transactionDate)

        if (updatedParentJvData.entityCode != 501) {
            aresMessagePublisher.emitPostJvToSage(
                PostJVToSageRequest(
                    parentJvId = Hashids.encode(updatedParentJvData.id!!),
                    performedBy = updatedParentJvData.createdBy!!

                )
            )
        }

        return Hashids.encode(parentJvData.id!!)
    }

    private suspend fun creatingLineItemsAndRequestToIncident(request: ParentJournalVoucherRequest, parentJvData: ParentJournalVoucher?, transactionDate: Date?) {
        request.jvLineItems.forEach { lineItem ->
            if (lineItem.tradePartyName.isNullOrEmpty() && lineItem.tradePartyId != null) {
                val data = railsClient.getListOrganizationTradePartyDetails(lineItem.tradePartyId!!)
                if (data.list.isNotEmpty()) {
                    lineItem.tradePartyName = data.list[0]["legal_business_name"].toString()
                }
            }

            if (lineItem.glCode.isNullOrEmpty() or lineItem.glCode.isNullOrBlank()) {
                throw AresException(AresError.ERR_1003, "GL code")
            }
            if (lineItem.type.isNullOrEmpty() or lineItem.type.isNullOrBlank()) {
                throw AresException(AresError.ERR_1003, "Type")
            }

            val jvLineItemData = JournalVoucher(
                id = null,
                jvNum = parentJvData?.jvNum!!,
                accMode = if (lineItem.accMode != null) lineItem.accMode else AccMode.OTHER,
                category = request.jvCategory,
                createdAt = request.createdAt,
                createdBy = request.createdBy,
                updatedAt = request.createdAt,
                updatedBy = request.createdBy,
                currency = request.currency,
                ledCurrency = request.ledCurrency,
                amount = lineItem.amount,
                ledAmount = lineItem.amount.multiply(request.exchangeRate),
                description = request.description,
                entityCode = lineItem.entityCode ?: request.entityCode,
                entityId = UUID.fromString(AresConstants.ENTITY_ID[lineItem.entityCode]),
                exchangeRate = request.exchangeRate,
                glCode = lineItem.glCode,
                parentJvId = parentJvData.id,
                type = lineItem.type,
                signFlag = getSignFlag(lineItem.type),
                status = JVStatus.APPROVED,
                tradePartyId = lineItem.tradePartyId,
                tradePartyName = lineItem.tradePartyName,
                validityDate = transactionDate,
                migrated = false,
                deletedAt = null
            )

            val jvLineItem = journalVoucherRepository.save(jvLineItemData)

            if (jvLineItem.tradePartyId != null && jvLineItem.accMode != AccMode.OTHER) {
                val accMode = AccMode.valueOf(jvLineItem.accMode!!.name)
                val signFlag = getSignFlag(lineItem.type)
                journalVoucherService.createJvAccUtil(jvLineItem, accMode, signFlag, true)
            }
        }
    }

    /**
     * Post JV to Sage based on I'd.
     * @param: I'd and Performed By
     * @return: Boolean
     */

    override suspend fun postJVToSage(parentJVId: Long, performedBy: UUID): Boolean {
        try {
            val parentJVDetails = parentJVRepository.findById(parentJVId) ?: throw AresException(AresError.ERR_1002, "JV")
            val jvLineItems = journalVoucherRepository.getJournalVoucherByParentJVId(parentJVId)

            if (parentJVDetails.entityCode == 501) {
                throw AresException(AresError.ERR_1526, "Not allowed to post jv of entity 501.")
            }

            if (parentJVDetails.status == JVStatus.POSTED) {
                throw AresException(AresError.ERR_1518, "")
            }

            var sageOrganization: SageOrganizationResponse?
            val jvLineItemsDetails = arrayListOf<JVLineItem>()

            jvLineItems.map { lineItem ->
                if (lineItem.tradePartyId != null) {
                    val organization = railsClient.getListOrganizationTradePartyDetails(lineItem.tradePartyId)

                    if (organization.list.isEmpty()) {
                        throw AresException(AresError.ERR_1530, "")
                    }

                    val sageOrganizationQuery = if (lineItem.accMode == AccMode.AR) "Select BPCNUM_0 from $sageDatabase.BPCUSTOMER where XX1P4PANNO_0='${organization.list[0]["registration_number"]}'" else "Select BPSNUM_0 from $sageDatabase.BPSUPPLIER where XX1P4PANNO_0='${organization.list[0]["registration_number"]}'"
                    val resultFromSageOrganizationQuery = Client.sqlQuery(sageOrganizationQuery)
                    val recordsForSageOrganization = ObjectMapper().readValue(resultFromSageOrganizationQuery, SageCustomerRecord::class.java)

                    if (recordsForSageOrganization.recordSet.isNullOrEmpty()) {
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
                                "Registration Number: ${organization.list[0]["registration_number"]}",
                                "Not Found BPR",
                                false
                            )
                        )
                        return false
                    }

                    val sageOrganizationFromSageId = if (lineItem.accMode == AccMode.AR) recordsForSageOrganization.recordSet?.get(0)?.sageOrganizationId else recordsForSageOrganization.recordSet?.get(0)?.sageSupplierId

                    sageOrganization = authClient.getSageOrganization(
                        SageOrganizationRequest(
                            organization.list[0]["serial_id"]!!.toString()
                        )
                    )

                    if (sageOrganization!!.sageOrganizationId.isNullOrEmpty()) {
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

                    if (sageOrganization!!.sageOrganizationId != sageOrganizationFromSageId) {
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
                                """Sage: $sageOrganizationFromSageId and Platform: ${sageOrganization!!.sageOrganizationId}""",
                                "sage serial organization id different on Sage and Cogoport Platform",
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

            val result: SageResponse = Client.postJVToSage(
                JVRequest
                (
                    parentJVDetails.category,
                    parentJVDetails.jvNum!!,
                    parentJVDetails.entityCode.toString(),
                    parentJVDetails.jvCodeNum!!,
                    parentJVDetails.currency!!,
                    parentJVDetails.jvNum!!,
                    parentJVDetails.transactionDate!!,
                    parentJVDetails.description!!,
                    parentJVDetails.exchangeRate!!.setScale(AresConstants.ROUND_OFF_DECIMAL_TO_2, RoundingMode.UP),
                    jvLineItemsDetails
                )
            )

            val processedResponse = XML.toJSONObject(result.response)
            val status = getStatus(processedResponse)

            if (status == 1) {
                val jvNumOnSage = "Select NUM_0 from $sageDatabase.GACCENTRY where NUM_0 = '${parentJVDetails.jvNum}'"
                val resultForJVNumOnSageQuery = Client.sqlQuery(jvNumOnSage)
                val mappedResponse = ObjectMapper().readValue<MutableMap<String, Any?>>(resultForJVNumOnSageQuery)
                val records = mappedResponse["recordset"] as? ArrayList<*>
                if (records?.size != 0) {
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
                            "404",
                            result.requestString,
                            result.response,
                            false
                        )
                    )
                    return false
                }
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
        } catch (aresException: AresException) {
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostJVToSage",
                    "Journal Voucher",
                    parentJVId,
                    "JOURNAL_VOUCHER",
                    "500",
                    parentJVId.toString(),
                    "${aresException.error.message} ${aresException.context}",
                    false
                )
            )
            throw aresException
        } catch (e: Exception) {
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostJVToSage",
                    "Journal Voucher",
                    parentJVId,
                    "JOURNAL_VOUCHER",
                    "500",
                    parentJVId.toString(),
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
            amount = journalVoucher.amount!!.setScale(AresConstants.ROUND_OFF_DECIMAL_TO_2, RoundingMode.UP),
            currency = journalVoucher.currency!!
        )
    }

    private fun getAccModeValue(accMode: AccMode): String {
        val accModeName = when (accMode) {
            AccMode.AP -> JVSageControls.AP.value
            AccMode.AR -> JVSageControls.AR.value
            AccMode.PDA -> JVSageControls.PDA.value
            AccMode.CSD -> JVSageControls.CSD.value
            AccMode.EMD -> JVSageControls.EMD.value
            AccMode.SUSA -> JVSageControls.SUSA.value
            AccMode.SUSS -> JVSageControls.SUSS.value
            AccMode.RE -> JVSageControls.RE.value
            AccMode.RI -> JVSageControls.RI.value
            AccMode.EMP -> JVSageControls.EMP.value
            AccMode.PREF -> JVSageControls.PREF.value
            AccMode.PC -> JVSageControls.PC.value
            AccMode.OTHER -> JVSageControls.OTHER.value
            AccMode.VTDS -> JVSageControls.VTDS.value
            else -> {
                throw AresException(AresError.ERR_1529, accMode.name)
            }
        }
        return accModeName
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

    private fun getZstatus(processedResponse: JSONObject?): String {
        val content = processedResponse?.getJSONObject("soapenv:Envelope")
            ?.getJSONObject("soapenv:Body")
            ?.getJSONObject("wss:runResponse")
            ?.getJSONObject("runReturn")
            ?.getJSONObject("resultXml")
            ?.get("content")

        val response = XML.toJSONObject(content.toString())
        val status = response?.getJSONObject("RESULT")
            ?.getJSONObject("GRP")
            ?.getJSONArray("FLD")
            ?.getJSONObject(1)
            ?.get("content")

        return status.toString()
    }

    override suspend fun getJvCategory(q: String?, pageLimit: Int?): List<JvCategory> {
        val query = util.toQueryString(q)
        val updatedPageLimit = pageLimit ?: 10
        return jvCategoryRepository.getJvCategory(query, updatedPageLimit)
    }

    override suspend fun getGLCode(entityCode: Int?, q: String?, pageLimit: Int?): List<GlCode> {
        val query = util.toQueryString(q)
        val updatedPageLimit = pageLimit ?: 10
        return glCodeRepository.getGLCode(entityCode, query, updatedPageLimit)
    }

    override suspend fun getGLCodeMaster(accMode: AccMode?, q: String?, pageLimit: Int?, entityCode: Int?): List<GlCodeMaster> {
        val updatedPageLimit = pageLimit ?: 10
        val query = util.toQueryString(q)
        val updatedAccMode = when (accMode) {
            null -> null
            else -> accMode.name
        }

        val countryEntityCode = mapOf(301 to listOf("IND", "USD"), 101 to listOf("IND", "USD"), 201 to listOf("NL", "USD"), 401 to listOf("SGP", "USD"), 501 to listOf("VN", "USD"))
        return glCodeMasterRepository.getGLCodeMaster(updatedAccMode, query, updatedPageLimit, countryEntityCode[entityCode])
    }

    override suspend fun getJournalCode(q: String?, pageLimit: Int?): List<JournalCode> {
        val updatedPageLimit = pageLimit ?: 10
        val query = util.toQueryString(q)
        return journalCodeRepository.getJournalCode(query, updatedPageLimit)
    }

    override suspend fun getAccountMode(q: String?, glCode: String?): List<HashMap<String, String>> {
        val query = util.toQueryString(q)
        val updatedGlCode = util.toQueryString(glCode)

        val uniqueAccountModes = glCodeMasterRepository.getDistinctAccType(query, updatedGlCode)

        val uniqueAccountModeList = mutableListOf<HashMap<String, String>>()

        uniqueAccountModes.forEachIndexed { index, it ->
            uniqueAccountModeList.add(hashMapOf("label" to it, "value" to it, "id" to index.toString()))
        }

        return uniqueAccountModeList
    }

    private suspend fun deleteJvFromSage(jvId: Long, jvNum: String): Boolean {
        try {
            val result = Client.deleteJvFromSage(jvNum)
            val processedResponse = XML.toJSONObject(result.response)
            val status = getZstatus(processedResponse)
            if (status == "DONE") {
                thirdPartyApiAuditService.createAudit(ThirdPartyApiAudit(null, ThirdPartyApiNames.DELETE_JV_FROM_SAGE.value, ThirdPartyApiType.JOURNAL_VOUCHERS.value, jvId, ThirdPartyObjectName.JOURNAL_VOUCHER.value, ThirdPartyResponseCode.SUCCESS.value, result.requestString, result.response, true))
                return true
            } else {
                thirdPartyApiAuditService.createAudit(ThirdPartyApiAudit(null, ThirdPartyApiNames.DELETE_JV_FROM_SAGE.value, ThirdPartyApiType.JOURNAL_VOUCHERS.value, jvId, ThirdPartyObjectName.JOURNAL_VOUCHER.value, ThirdPartyResponseCode.FAILURE.value, result.requestString, result.response, false))
            }
        } catch (err: Exception) {
            thirdPartyApiAuditService.createAudit(ThirdPartyApiAudit(null, ThirdPartyApiNames.DELETE_JV_FROM_SAGE.value, ThirdPartyApiType.JOURNAL_VOUCHERS.value, jvId, ThirdPartyObjectName.JOURNAL_VOUCHER.value, ThirdPartyResponseCode.FAILURE.value, jvNum, err.toString(), false))
        }
        return false
    }

    @Transactional
    override suspend fun createTdsAsJvForBills(
        currency: String?,
        ledCurrency: String,
        tdsAmount: BigDecimal,
        tdsLedAmount: BigDecimal,
        createdBy: UUID?,
        createdByUserType: String?,
        accountUtilization: AccountUtilization?,
        exchangeRate: BigDecimal?,
        paymentTransactionDate: Date,
        lineItemProps: MutableList<HashMap<String, Any?>>,
        utr: String?,
        payCurrTds: BigDecimal?,
        payLocTds: BigDecimal?
    ): Long? {
        val jvNum = getJvNumber()
        var parentJournalVoucher = ParentJournalVoucher(
            id = null,
            status = JVStatus.APPROVED,
            category = "VTDS",
            jvNum = jvNum,
            // picking date of payments
            transactionDate = paymentTransactionDate,
            validityDate = accountUtilization?.transactionDate,
            currency = currency,
            ledCurrency = ledCurrency,
            entityCode = accountUtilization?.entityCode,
            exchangeRate = exchangeRate?.setScale(AresConstants.DECIMAL_NUMBER_UPTO, RoundingMode.HALF_DOWN),
            description = "TDS AGAINST ${accountUtilization?.documentValue}",
            createdBy = createdBy,
            updatedBy = createdBy,
            jvCodeNum = "VTDS",
            isUtilized = true
        )

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PARENT_JOURNAL_VOUCHERS,
                objectId = parentJournalVoucher.id,
                actionName = AresConstants.CREATE,
                data = parentJournalVoucher,
                performedBy = createdBy.toString(),
                performedByUserType = createdByUserType
            )
        )

        parentJournalVoucher = parentJVRepository.save(parentJournalVoucher)

        return journalVoucherService.createTdsJvLineItems(parentJournalVoucher, accountUtilization, lineItemProps, tdsAmount, tdsLedAmount, createdByUserType, payCurrTds, payLocTds, utr)
    }

    override suspend fun bulkPostingJvToSage() {
        val parentJvIds = parentJVRepository.getParentJournalVoucherIds()
        logger().info("size of jv posting : ${parentJvIds?.size}")

        if (!parentJvIds.isNullOrEmpty()) {
            parentJvIds.map {
                aresMessagePublisher.emitPostJvToSage(
                    PostJVToSageRequest(
                        parentJvId = Hashids.encode(it),
                        performedBy = AresConstants.ARES_USER_ID
                    )
                )
            }
        }
    }

    override suspend fun bulkJvDeletion(jvNumbers: List<String>) {
        val parentJvDetails = parentJVRepository.getParentJournalVoucherByJvNums(jvNumbers)

        if (!parentJvDetails.isNullOrEmpty()) {
            val filteredJvs = parentJvDetails.filter { it.isUtilized == false }
            parentJVRepository.deleteAll(filteredJvs)

            val jvData = journalVoucherRepository.findByJvNums(filteredJvs.map { it.jvNum!! })
            if (!jvData.isNullOrEmpty()) {
                val accUtilData = accountUtilizationRepo.getAccountUtilizationsByDocumentNo(jvData.filter { it.accMode != AccMode.OTHER && it.accMode != null }.map { it.id!! }, jvData.map { it.category })
                accountUtilizationRepo.deleteAll(accUtilData)
                journalVoucherRepository.deleteAll(jvData)
            }
        }
    }
}
