package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.AuthClient
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.api.settlement.entity.ThirdPartyApiAudit
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.model.ICJVUpdateRequest
import com.cogoport.ares.api.settlement.repository.JournalVoucherParentRepo
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.api.settlement.service.interfaces.ICJVService
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.settlement.service.interfaces.ThirdPartyApiAuditService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.sage.SageCustomerRecord
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVSageAccount
import com.cogoport.ares.model.settlement.enums.JVSageControls
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.enums.SageGLCodes
import com.cogoport.ares.model.settlement.request.ICJVRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentICJVRequest
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
import com.cogoport.hades.model.incident.enums.IncidentStatus
import com.cogoport.hades.model.incident.enums.IncidentType
import com.cogoport.hades.model.incident.enums.Source
import com.cogoport.hades.model.incident.request.CreateIncidentRequest
import com.cogoport.hades.model.incident.request.UpdateIncidentRequest
import com.cogoport.hades.model.incident.response.ICJVEntry
import com.cogoport.plutus.model.invoice.SageOrganizationRequest
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.json.JSONObject
import org.json.XML
import java.sql.Date
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.UUID
import javax.transaction.Transactional

@Singleton
open class ICJVServiceImpl : ICJVService {

    @Inject
    lateinit var authClient: AuthClient

    @Inject
    lateinit var journalVoucherParentRepo: JournalVoucherParentRepo

    @Inject
    lateinit var sequenceGeneratorImpl: SequenceGeneratorImpl

    @Inject
    lateinit var journalVoucherConverter: JournalVoucherMapper

    @Inject
    lateinit var journalVoucherService: JournalVoucherService

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject
    lateinit var hadesClient: HadesClient

    @Inject
    lateinit var railsClient: RailsClient

    @Inject
    lateinit var auditService: AuditService

    @Inject
    lateinit var thirdPartyApiAuditService: ThirdPartyApiAuditService

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun createICJV(request: ParentICJVRequest): String {
        validateCreateRequest(request)

        val parentJvNumber = getJvNumber()
        request.jvNum = parentJvNumber
        request.status = JVStatus.PENDING
        val parentData = journalVoucherConverter.convertParentICJVRequestToParentJV(request)
        val parentJvData = journalVoucherParentRepo.save(parentData)

        val incidentModelData = mutableListOf<ICJVEntry>()

        request.list.mapIndexed { index, it ->
            it.jvNum = parentJvNumber + "/L${index + 1}"
            it.parentJvId = parentJvData.id.toString()
            it.status = JVStatus.PENDING
            it.category = request.category

            if (it.entityId == null) {
                val data = railsClient.getCogoEntity(it.entityCode.toString())
                if (data.list.isNotEmpty()) {
                    it.entityId = UUID.fromString(data.list[0]["id"].toString())
                }
            }

            if (it.tradePartyName == null && it.tradePartyId != null) {
                val data = railsClient.getListOrganizationTradePartyDetails(it.tradePartyId!!)
                if (data.list.isNotEmpty()) {
                    it.tradePartyName = data.list[0]["legal_business_name"].toString()
                }
            }

            val jv = convertToJournalVoucherEntity(request, it)
            val jvEntity = journalVoucherService.createJV(jv)

            val incidentRequestModel = journalVoucherConverter.convertJournalVoucherModelToICJVEntry(jvEntity)
            incidentRequestModel.id = Hashids.encode(jvEntity.id!!)
            incidentModelData.add(incidentRequestModel)
        }

        sendToIncidentManagement(parentJvData, incidentModelData)

        return Hashids.encode(parentJvData.id!!)
    }

    override suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<ParentJournalVoucherResponse> {
        val documentEntity = journalVoucherParentRepo.getListVouchers(
            jvListRequest.status,
            jvListRequest.category,
            jvListRequest.query,
            jvListRequest.page,
            jvListRequest.pageLimit,
            jvListRequest.sortType,
            jvListRequest.sortBy
        )
        val totalRecords =
            journalVoucherParentRepo.countDocument(
                jvListRequest.status,
                jvListRequest.category,
                jvListRequest.query
            )

        val jvList = mutableListOf<ParentJournalVoucherResponse>()
        documentEntity.forEach { doc ->
            val countOfChildrenJvs = journalVoucherRepository.getCountOfJournalVoucherByParentJVId(doc.id!!)
            val jvData = journalVoucherConverter.convertICJVEntityToModel((doc))
            jvData.id = Hashids.encode(jvData.id.toLong())
            jvData.countOfChildrenJvs = countOfChildrenJvs
            jvList.add(jvData)
        }

        return ResponseList(
            list = jvList,
            totalPages = Utilities.getTotalPages(totalRecords, jvListRequest.pageLimit),
            totalRecords = totalRecords,
            pageNo = jvListRequest.page
        )
    }

    override suspend fun getJournalVoucherByParentJVId(parentId: Long): List<JournalVoucherResponse> {
        val documentEntity = journalVoucherRepository.getJournalVoucherByParentJVId(parentId)
        return documentEntity
    }
    private fun validateCreateRequest(request: ParentICJVRequest) {
        if (request.createdBy == null) throw AresException(AresError.ERR_1003, "Created By")
    }

    private suspend fun getJvNumber() =
        SequenceSuffix.JV.prefix + "/" + Utilities.getFinancialYear() + "/" + sequenceGeneratorImpl.getPaymentNumber(
            SequenceSuffix.JV.prefix
        )

    private fun convertToJournalVoucherEntity(parentICJV: ParentICJVRequest, request: ICJVRequest): JournalVoucher {
        val jv = journalVoucherConverter.convertICJVRequestToJournalVoucher(request)
        jv.status = JVStatus.PENDING
        jv.currency = parentICJV.currency
        jv.amount = parentICJV.amount
        jv.accMode = parentICJV.accMode
        jv.validityDate = parentICJV.validityDate
        jv.ledCurrency = parentICJV.ledCurrency
        jv.exchangeRate = parentICJV.exchangeRate
        jv.createdBy = parentICJV.createdBy
        jv.updatedBy = parentICJV.createdBy
        jv.description = parentICJV.description
        jv.type = request.type.lowercase()
        return jv
    }

    private fun getSignFlag(type: String): Short {
        return when (type.uppercase()) {
            "CREDIT" -> { -1 }
            "DEBIT" -> { 1 }
            else -> {
                throw AresException(AresError.ERR_1009, "JV type")
            }
        }
    }

    private suspend fun sendToIncidentManagement(
        parentJvData: ParentJournalVoucher,
        data: MutableList<ICJVEntry>
    ) {
        val interCompanyJournalVoucherRequest = journalVoucherConverter.convertParentJVToICJVApproval(parentJvData)
        interCompanyJournalVoucherRequest.id = Hashids.encode(parentJvData.id!!)
        val formattedDate = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(interCompanyJournalVoucherRequest.validityDate)
        interCompanyJournalVoucherRequest.validityDate = Date.valueOf(formattedDate)
        interCompanyJournalVoucherRequest.list = data
        val incidentData =
            IncidentData(
                organization = null,
                journalVoucherRequest = null,
                tdsRequest = null,
                creditNoteRequest = null,
                settlementRequest = null,
                bankRequest = null,
                interCompanyJournalVoucherRequest = interCompanyJournalVoucherRequest
            )

        val type = when (parentJvData.category) {
            JVCategory.ICJV -> IncidentType.INTER_COMPANY_JOURNAL_VOUCHER_APPROVAL
            JVCategory.ICJVBT -> IncidentType.INTER_COMPANY_JOURNAL_VOUCHER_BANK_TRANSFER_APPROVAL
            JVCategory.ICJVC -> IncidentType.INTER_COMPANY_JOURNAL_VOUCHER_CONTRA_APPROVAL
            else -> return
        }

        val clientRequest = CreateIncidentRequest(
            type = type,
            description = "Inter Company Journal Voucher Approval",
            data = incidentData,
            source = Source.SETTLEMENT,
            entityId = null,
            createdBy = parentJvData.createdBy!!
        )
        hadesClient.createIncident(clientRequest)
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun updateICJV(request: ICJVUpdateRequest): String {
        val parentJvId = Hashids.decode(request.parentJvId!!)[0]
        val jvCategories = listOf(JVCategory.ICJVBT, JVCategory.ICJVC)
        // Update Journal Voucher
        val parentJvData = journalVoucherParentRepo.findById(parentJvId) ?: throw AresException(AresError.ERR_1519, "")

        when (parentJvData.status == request.status!!) {
            true -> throw AresException(AresError.ERR_1520, "${request.status}")
            false -> parentJvData.status = request.status
        }

        journalVoucherParentRepo.update(parentJvData)

        val childJvData = journalVoucherRepository.getJVModelByParentJVId(parentJvId)

        childJvData.map { it ->
            it.status = request.status
            it.updatedAt = Timestamp.from(Instant.now())
            it.updatedBy = request.performedBy
            journalVoucherRepository.update(it)

            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.JOURNAL_VOUCHERS,
                    objectId = it.id,
                    actionName = AresConstants.UPDATE,
                    data = it,
                    performedBy = it.createdBy.toString(),
                    performedByUserType = null
                )
            )

            // Insert JV in account_utilizations
            val accMode = it.accMode
            val signFlag = getSignFlag(it.type!!)
            if (!jvCategories.contains(parentJvData.category)) {
                journalVoucherService.createJvAccUtil(it, accMode, signFlag)
            }
        }
        // Update Incident status on incident management

        val incidentStatus = when (request.status.name) {
            "APPROVED" -> IncidentStatus.APPROVED
            "REJECTED" -> IncidentStatus.REJECTED
            else -> IncidentStatus.REQUESTED
        }
        hadesClient.updateIncident(
            request = UpdateIncidentRequest(
                status = incidentStatus,
                data = null,
                remark = request.remark,
                updatedBy = request.performedBy!!
            ),
            id = request.incidentId
        )

        return request.incidentId!!
    }

    override suspend fun postICJVToSage(parentICJVId: Long, performedBy: UUID): Boolean {
        try {
            val parentICJVDetails = journalVoucherParentRepo.findById(parentICJVId) ?: throw AresException(AresError.ERR_1002, "")
            val jvDetails = journalVoucherRepository.getJVModelByParentJVId(parentICJVId)

            if (parentICJVDetails.status != JVStatus.UTILIZED && parentICJVDetails.status != JVStatus.POSTING_FAILED) {
                throw AresException(AresError.ERR_1516, "")
            }

            if (parentICJVDetails.status == JVStatus.POSTED) {
                throw AresException(AresError.ERR_1518, "")
            }

            val jv1: JournalVoucher // to be posted with BPR
            val jv2: JournalVoucher // posted without BPR

            if ((jvDetails[0].accMode == AccMode.AR && jvDetails[0].type == "CREDIT") || (jvDetails[0].accMode == AccMode.AP && jvDetails[0].type == "DEBIT")) {
                jv1 = jvDetails[0]
                jv2 = jvDetails[1]
            } else {
                jv1 = jvDetails[1]
                jv2 = jvDetails[0]
            }
            val result = postEachJV(jv1, performedBy, "JV_1")
            if (result) {
                return postEachJV(jv2, performedBy, "JV_2")
            }
        } catch (exception: SageException) {
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostICJVToSage",
                    "PostICJV",
                    parentICJVId,
                    "ICJV/BT/C",
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
                    "PostICJVToSage",
                    "PostICJV",
                    parentICJVId,
                    "ICJV/BT/C",
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

    private suspend fun postEachJV(jv: JournalVoucher, performedBy: UUID, jvCount: String): Boolean {
        val sageOrganizationId: String = ""
        if (jvCount == "JV_1" && jv.category == JVCategory.ICJV) {
            val organization = railsClient.getListOrganizationTradePartyDetails(jv.tradePartyId!!)

            val sageOrganization = authClient.getSageOrganization(
                SageOrganizationRequest(
                    organization.list[0]["serial_id"]!!.toString(),
                    if (jv.accMode == AccMode.AP) "service_provider" else "importer_exporter"
                )
            )
            val sageOrganizationQuery =
                "Select BPCNUM_0 from $sageDatabase.BPCUSTOMER where XX1P4PANNO_0='${organization.list[0]["registration_number"]}'"

            val resultFromSageOrganizationQuery = Client.sqlQuery(sageOrganizationQuery)
            val recordsForSageOrganization = ObjectMapper()
                .readValue(resultFromSageOrganizationQuery, SageCustomerRecord::class.java)
            val sageOrganizationFromSageId = recordsForSageOrganization.recordSet?.get(0)?.sageOrganizationId

            if (sageOrganization.sageOrganizationId.isNullOrEmpty()) {
                journalVoucherRepository.updateStatus(jv.id!!, JVStatus.POSTING_FAILED, performedBy)
                thirdPartyApiAuditService.createAudit(
                    ThirdPartyApiAudit(
                        null,
                        "PostJVToSage",
                        "Journal Voucher",
                        jv.id,
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
        }

        lateinit var result: SageResponse
        var mapDestinationDocumentValue = ""

        if (jv.category == JVCategory.ICJV) {
            val destinationDocumentValue = settlementRepository.findBySourceIdAndSourceType(
                jv.id!!,
                listOf(
                    SettlementType.valueOf(jv.category.toString())
                )
            ) // only in ICJV CASE
            mapDestinationDocumentValue = destinationDocumentValue.map { it?.destinationId }.joinToString(",")
        }

        val jvLineItemDetails = getJvLineItem(jv, jvCount)
        jvLineItemDetails.sageBPRNumber = if (jvCount == "JV_1" && jv.category == JVCategory.ICJV) sageOrganizationId else ""

        result = Client.postJVToSage(
            JVRequest(
                jvEntryType = JVEntryType.MISC,
                jvNumber = jv.jvNum,
                entityCode = jv.entityCode.toString(),
                jvType = JVType.MISC,
                currency = jv.currency!!,
                destinationDocumentValue = mapDestinationDocumentValue,
                transactionDate = jv.createdAt!!,
                description = jv.description!!,
                lineItems = arrayListOf(jvLineItemDetails, getJvGLLineItem(jv))
            )
        )

        val processedResponse = XML.toJSONObject(result.response)
        val status = getStatus(processedResponse)

        if (status == 1) {
            journalVoucherRepository.updateStatus(jv.id!!, JVStatus.POSTED, performedBy)
            journalVoucherParentRepo.updateStatus(jv.parentJvId!!, JVStatus.POSTED, performedBy)
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostICJVToSage",
                    "PostICJV",
                    jv.id,
                    "${jv.category}",
                    "200",
                    result.requestString,
                    result.response,
                    true
                )
            )
            return true
        } else {
            journalVoucherRepository.updateStatus(jv.id!!, JVStatus.POSTING_FAILED, performedBy)
            journalVoucherParentRepo.updateStatus(jv.parentJvId!!, JVStatus.POSTING_FAILED, performedBy)
            thirdPartyApiAuditService.createAudit(
                ThirdPartyApiAudit(
                    null,
                    "PostICJVToSage",
                    "PostICJV",
                    jv.id,
                    "${jv.category}",
                    "200",
                    result.requestString,
                    result.response,
                    false
                )
            )
            return false
        }
    }

    private fun getJvLineItem(journalVoucher: JournalVoucher, jvCount: String): JVLineItem {
        var glCode = ""
        var accMode = ""
        if (jvCount == "JV_1" && journalVoucher.category == JVCategory.ICJV) {
            glCode = if (journalVoucher.accMode == AccMode.AP) JVSageAccount.AP.value else JVSageAccount.AR.value
            accMode = if (journalVoucher.accMode == AccMode.AP) JVSageControls.AP.value else JVSageControls.AR.value
        }

        return JVLineItem(
            acc = glCode,
            accMode = accMode,
            sageBPRNumber = "",
            description = "",
            signFlag = getSignFlag(journalVoucher.type.toString().uppercase()).toInt(),
            amount = journalVoucher.amount!!,
            currency = journalVoucher.currency!!
        )
    }

    private fun getJvGLLineItem(journalVoucher: JournalVoucher): JVLineItem {
        return JVLineItem(
            SageGLCodes.ICJV.value,
            "",
            "",
            "",
            getSignFlag(journalVoucher.type.toString().uppercase()).toInt() * -1,
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
