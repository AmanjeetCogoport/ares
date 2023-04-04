package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.model.JournalVoucherApproval
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.sage.SageCustomerRecord
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVSageAccount
import com.cogoport.ares.model.settlement.enums.JVSageControls
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.enums.SageGLCodes
import com.cogoport.ares.model.settlement.request.JournalVoucherReject
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.sage.Client
import com.cogoport.brahma.sage.SageException
import com.cogoport.brahma.sage.model.request.JVEntryType
import com.cogoport.brahma.sage.model.request.JVLineItem
import com.cogoport.brahma.sage.model.request.JVRequest
import com.cogoport.brahma.sage.model.request.JVType
import com.cogoport.brahma.sage.model.request.SageResponse
import com.cogoport.hades.client.HadesClient
import com.cogoport.hades.model.incident.IncidentData
import com.cogoport.hades.model.incident.Organization
import com.cogoport.hades.model.incident.enums.IncidentType
import com.cogoport.hades.model.incident.request.CreateIncidentRequest
import com.cogoport.hades.model.incident.request.UpdateIncidentRequest
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.json.JSONObject
import org.json.XML
import java.math.BigDecimal
import java.sql.Date
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.UUID
import javax.transaction.Transactional

@Singleton
open class JournalVoucherServiceImpl : JournalVoucherService {

    @Inject
    lateinit var journalVoucherConverter: JournalVoucherMapper

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var hadesClient: HadesClient

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject
    lateinit var auditService: AuditService

    @Inject
    lateinit var aresMessagePublisher: AresMessagePublisher

    @Inject
    lateinit var thirdPartyApiAuditService: ThirdPartyApiAuditService

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var railsClient: RailsClient

    @Inject
    lateinit var authClient: AuthClient

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    /**
     * Get List of JVs based on input filters.
     * @param: jvListRequest
     * @return: ResponseList
     */
    override suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<JournalVoucherResponse> {

        val documentEntity = journalVoucherRepository.getListVouchers(
            jvListRequest.status,
            jvListRequest.category?.name,
            jvListRequest.type,
            jvListRequest.query,
            jvListRequest.page,
            jvListRequest.pageLimit,
            jvListRequest.sortType,
            jvListRequest.sortBy,
            jvListRequest.entityCode
        )
        val totalRecords =
            journalVoucherRepository.countDocument(
                jvListRequest.status,
                jvListRequest.category?.name,
                jvListRequest.type,
                jvListRequest.query,
                jvListRequest.entityCode
            )
        val jvList = mutableListOf<JournalVoucherResponse>()
        documentEntity.forEach { doc ->
            jvList.add(journalVoucherConverter.convertToModelResponse((doc)))
        }

        return ResponseList(
            list = jvList,
            totalPages = Utilities.getTotalPages(totalRecords, jvListRequest.pageLimit),
            totalRecords = totalRecords,
            pageNo = jvListRequest.page
        )
    }

    /**
     * Create a journal voucher and add it to account_utilizationns.
     * @param: journalVoucher
     * @return: com.cogoport.ares.api.settlement.entity.JournalVoucher
     */
    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun createJournalVoucher(request: JournalVoucherRequest): String {
        // validate request
        validateCreateRequest(request)

        // create Journal Voucher
        request.jvNum = getJvNumber()
        val jv = convertToJournalVoucherEntity(request)
        val jvEntity = createJV(jv)

        request.id = Hashids.encode(jvEntity.id!!)
        if (request.status == JVStatus.PENDING) {
            // Send to Incident Management
            val formatedDate = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(request.validityDate)
            val incidentRequestModel = journalVoucherConverter.convertToIncidentModel(request)
            incidentRequestModel.validityDate = Date.valueOf(formatedDate)
            sendToIncidentManagement(request, incidentRequestModel)
        } else {
            // Insert JV in account_utilizations
            val signFlag = getSignFlag(request.accMode, request.type)
            createJvAccUtil(jvEntity, request.accMode, signFlag)
        }

        return request.id!!
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun approveJournalVoucher(request: JournalVoucherApproval): String {
        val jvId = Hashids.decode(request.journalVoucherData!!.id)[0]
        // Update Journal Voucher
        val jvEntity = updateJournalVoucher(jvId, request.performedBy, request.remark)

        // Insert JV in account_utilizations
        val accMode = AccMode.valueOf(request.journalVoucherData.accMode)
        val signFlag = getSignFlag(accMode, request.journalVoucherData.type)
        val accUtilEntity = createJvAccUtil(jvEntity, accMode, signFlag)

        // Update Incident status on incident management
        hadesClient.updateIncident(
            request = UpdateIncidentRequest(
                status = com.cogoport.hades.model.incident.enums.IncidentStatus.APPROVED,
                data = null,
                remark = request.remark,
                updatedBy = request.performedBy!!
            ),
            id = request.incidentId
        )
        return request.incidentId!!
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun rejectJournalVoucher(request: JournalVoucherReject): String {
        val jvId = Hashids.decode(request.journalVoucherId!!)[0]
        journalVoucherRepository.reject(jvId, request.performedBy!!, request.remark)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.JOURNAL_VOUCHERS,
                objectId = jvId,
                actionName = AresConstants.UPDATE,
                data = mapOf("id" to jvId, "status" to JVStatus.REJECTED),
                performedBy = request.performedBy.toString(),
                performedByUserType = null
            )
        )
        hadesClient.updateIncident(
            request = UpdateIncidentRequest(
                status = com.cogoport.hades.model.incident.enums.IncidentStatus.REJECTED,
                data = null,
                remark = request.remark,
                updatedBy = request.performedBy!!
            ),
            id = request.incidentId
        )
        return request.incidentId!!
    }

    override suspend fun updateJournalVoucherStatus(id: Long, status: JVStatus, performedBy: UUID, performedByUserType: String?) {
        journalVoucherRepository.updateStatus(id, status, performedBy)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.JOURNAL_VOUCHERS,
                objectId = id,
                actionName = AresConstants.UPDATE,
                data = mapOf("id" to id, "status" to status),
                performedBy = performedBy.toString(),
                performedByUserType = performedByUserType
            )
        )
    }

    private suspend fun updateJournalVoucher(jvId: Long, performedBy: UUID?, remark: String?): JournalVoucher {
        val jvEntity = journalVoucherRepository.findById(jvId) ?: throw AresException(AresError.ERR_1002, "journal_voucher_id: $jvId")
        jvEntity.status = JVStatus.APPROVED
        jvEntity.updatedAt = Timestamp.from(Instant.now())
        jvEntity.updatedBy = performedBy
        jvEntity.description = remark
        journalVoucherRepository.update(jvEntity)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.JOURNAL_VOUCHERS,
                objectId = jvEntity.id,
                actionName = AresConstants.UPDATE,
                data = jvEntity,
                performedBy = jvEntity.createdBy.toString(),
                performedByUserType = null
            )
        )
        return jvEntity
    }

    override suspend fun createJvAccUtil(request: JournalVoucher, accMode: AccMode, signFlag: Short): AccountUtilization {
        val accountAccUtilizationRequest = AccountUtilization(
            id = null,
            documentNo = request.id!!,
            entityCode = request.entityCode!!,
            orgSerialId = 1,
            sageOrganizationId = null,
            organizationId = request.tradePartyId,
            taggedOrganizationId = null,
            tradePartyMappingId = null,
            organizationName = request.tradePartyName,
            accType = AccountType.valueOf(request.category.toString()),
            accMode = accMode,
            signFlag = signFlag,
            currency = request.currency!!,
            ledCurrency = request.ledCurrency,
            amountCurr = request.amount ?: BigDecimal.ZERO,
            amountLoc = request.amount?.multiply(request.exchangeRate) ?: BigDecimal.ZERO,
            payCurr = BigDecimal.ZERO,
            payLoc = BigDecimal.ZERO,
            taxableAmount = BigDecimal.ZERO,
            zoneCode = "WEST",
            documentStatus = DocumentStatus.FINAL,
            documentValue = request.jvNum,
            dueDate = request.validityDate,
            transactionDate = request.validityDate,
            serviceType = ServiceType.NA.toString(),
            category = null,
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now()),
            accCode = AresModelConstants.AR_ACCOUNT_CODE,
            migrated = false
        )
        val accUtilObj = accountUtilizationRepository.save(accountAccUtilizationRequest)

        aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accountAccUtilizationRequest.organizationId))

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accUtilObj.id,
                actionName = AresConstants.CREATE,
                data = accUtilObj,
                performedBy = request.createdBy.toString(),
                performedByUserType = null
            )
        )
        return accUtilObj
    }

    /**
     * Get JV number from generator in fixed format
     * @return: String
     */
    private suspend fun getJvNumber() =
        SequenceSuffix.JV.prefix + "/" + Utilities.getFinancialYear() + "/" + sequenceGeneratorImpl.getPaymentNumber(
            SequenceSuffix.JV.prefix
        )

    override suspend fun createJV(jv: JournalVoucher): JournalVoucher {
        jv.migrated = false
        val jvObj = journalVoucherRepository.save(jv)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.JOURNAL_VOUCHERS,
                objectId = jvObj.id,
                actionName = AresConstants.CREATE,
                data = jvObj,
                performedBy = jvObj.createdBy.toString(),
                performedByUserType = null
            )
        )
        return jvObj
    }

    private fun validateCreateRequest(request: JournalVoucherRequest) {
        if (request.createdBy == null) throw AresException(AresError.ERR_1003, "Created By")
    }

    private fun convertToJournalVoucherEntity(request: JournalVoucherRequest): JournalVoucher {
        request.status = if (request.currency == "INR" && request.amount <= 50.toBigDecimal())
            JVStatus.APPROVED else JVStatus.PENDING
        val jv = journalVoucherConverter.convertRequestToEntity(request)
        jv.createdAt = Timestamp.from(Instant.now())
        jv.updatedAt = Timestamp.from(Instant.now())
        jv.type = request.type.lowercase()
        return jv
    }

    private suspend fun sendToIncidentManagement(
        request: JournalVoucherRequest,
        data: com.cogoport.hades.model.incident.JournalVoucher
    ) {
        val incidentData =
            IncidentData(
                organization = Organization(
                    id = request.tradePartyId,
                    businessName = request.tradePartyName,
                    tradePartyType = null,
                    tradePartyName = null
                ),
                journalVoucherRequest = data,
                tdsRequest = null,
                creditNoteRequest = null,
                settlementRequest = null,
                bankRequest = null
            )
        val clientRequest = CreateIncidentRequest(
            type = IncidentType.JOURNAL_VOUCHER_APPROVAL,
            description = "Journal Voucher Approval",
            data = incidentData,
            createdBy = request.createdBy!!
        )
        hadesClient.createIncident(clientRequest)
    }

    /**
     * Return Sign Flag on the basis of account mode and type
     * @param: accMode
     * @param: type
     * @return: Short
     */
    private fun getSignFlag(accMode: AccMode, type: String): Short {
        return when (type) {
            "CREDIT" -> { -1 }
            "DEBIT" -> { 1 }
            else -> {
                throw AresException(AresError.ERR_1009, "JV type")
            }
        }
    }

    override suspend fun postJVToSage(jvId: Long, performedBy: UUID): Boolean {
        try {
            val jvDetails = journalVoucherRepository.findById(jvId) ?: throw AresException(AresError.ERR_1002, "")

            if (jvDetails.status != JVStatus.UTILIZED && jvDetails.status != JVStatus.POSTING_FAILED) {
                throw AresException(AresError.ERR_1516, "")
            }

            if (jvDetails.status == JVStatus.POSTED) {
                throw AresException(AresError.ERR_1518, "")
            }

            val organization = railsClient.getListOrganizationTradePartyDetails(jvDetails.tradePartyId!!)

            val sageOrganization = authClient.getSageOrganization(
                SageOrganizationRequest(
                    organization.list[0]["serial_id"]!!.toString(),
                    if (jvDetails.accMode == AccMode.AP) "service_provider" else "importer_exporter"
                )
            )

            val sageOrganizationQuery = "Select BPCNUM_0 from $sageDatabase.BPCUSTOMER where XX1P4PANNO_0='${organization.list[0]["registration_number"]}'"
            val resultFromSageOrganizationQuery = Client.sqlQuery(sageOrganizationQuery)
            val recordsForSageOrganization = ObjectMapper().readValue(resultFromSageOrganizationQuery, SageCustomerRecord::class.java)
            val sageOrganizationFromSageId = recordsForSageOrganization.recordSet?.get(0)?.sageOrganizationId

            if (sageOrganization.sageOrganizationId.isNullOrEmpty()) {
                journalVoucherRepository.updateStatus(jvId, JVStatus.POSTING_FAILED, performedBy)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostJVToSage",
                        "Journal Voucher",
                        jvId,
                        "JOURNAL_VOUCHER",
                        "200",
                        sageOrganization.toString(),
                        "Sage organization not present",
                        true
                    )
                )
                return false
            }

            if (sageOrganization.sageOrganizationId != sageOrganizationFromSageId)
                return false

            lateinit var result: SageResponse

            val destinationDocumentValue = settlementRepository.findBySourceIdAndSourceType(jvId, listOf(SettlementType.valueOf(jvDetails.category.toString())))

            val mapDestinationDocumentValue = destinationDocumentValue.map {
                it?.destinationId
            }.joinToString(",")

            val jvLineItemDetails = getJvLineItem(jvDetails)
            jvLineItemDetails.sageBPRNumber = sageOrganization.sageOrganizationId!!

            result = Client.postJVToSage(
                JVRequest
                (
                    if (jvDetails.category == JVCategory.EXCH.name) JVEntryType.MCTTV else JVEntryType.MISC,
                    jvDetails.jvNum,
                    jvDetails.entityCode.toString(),
                    JVType.MISC,
                    jvDetails.currency!!,
                    mapDestinationDocumentValue,
                    jvDetails.createdAt!!,
                    jvDetails.description!!,
                    arrayListOf(jvLineItemDetails, getJvGLLineItem(jvDetails))
                )
            )

            val processedResponse = XML.toJSONObject(result.response)
            val status = getStatus(processedResponse)

            if (status == 1) {
                journalVoucherRepository.updateStatus(jvId, JVStatus.POSTED, performedBy)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostJVToSage",
                        "Journal Voucher",
                        jvId,
                        "JOURNAL_VOUCHER",
                        "200",
                        result.requestString,
                        result.response,
                        true
                    )
                )
                return true
            } else {
                journalVoucherRepository.updateStatus(jvId, JVStatus.POSTING_FAILED, performedBy)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostJVToSage",
                        "Journal Voucher",
                        jvId,
                        "JOURNAL_VOUCHER",
                        "200",
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
                    "Journal Voucher", jvId,
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
                    jvId,
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

    private fun getJvLineItem(journalVoucher: JournalVoucher): JVLineItem {
        return JVLineItem(
            acc = if (journalVoucher.accMode == AccMode.AP) JVSageAccount.AP.value else JVSageAccount.AR.value,
            accMode = if (journalVoucher.accMode == AccMode.AP) JVSageControls.AP.value else JVSageControls.AR.value,
            sageBPRNumber = "",
            description = "",
            signFlag = getSignFlag(journalVoucher.accMode!!, journalVoucher.type.toString().uppercase()).toInt(),
            amount = journalVoucher.amount!!,
            currency = journalVoucher.currency!!
        )
    }

    private fun getJvGLLineItem(journalVoucher: JournalVoucher): JVLineItem {
        val glCode = when (journalVoucher.category) {
            JVCategory.JVNOS.name -> SageGLCodes.JVNOS
            JVCategory.EXCH.name -> SageGLCodes.EXCH
            JVCategory.ROFF.name -> SageGLCodes.ROFF
            JVCategory.WOFF.name -> SageGLCodes.WOFF
            JVCategory.ICJV.name -> SageGLCodes.ICJV
            else -> { throw AresException(AresError.ERR_1517, journalVoucher.category.toString()) }
        }
        return JVLineItem(
            glCode.value,
            "",
            "",
            "",
            getSignFlag(journalVoucher.accMode!!, journalVoucher.type.toString().uppercase()).toInt() * -1,
            amount = journalVoucher.amount!!,
            currency = journalVoucher.currency!!
        )
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
