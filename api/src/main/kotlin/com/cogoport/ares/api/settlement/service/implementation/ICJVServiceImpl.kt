package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.model.ICJVUpdateRequest
import com.cogoport.ares.api.settlement.repository.JournalVoucherParentRepo
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.service.interfaces.ICJVService
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.request.ICJVRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentICJVRequest
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.hades.client.HadesClient
import com.cogoport.hades.model.incident.IncidentData
import com.cogoport.hades.model.incident.enums.IncidentStatus
import com.cogoport.hades.model.incident.enums.IncidentType
import com.cogoport.hades.model.incident.enums.Source
import com.cogoport.hades.model.incident.request.CreateIncidentRequest
import com.cogoport.hades.model.incident.request.UpdateIncidentRequest
import com.cogoport.hades.model.incident.response.ICJVEntry
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Date
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.transaction.Transactional

@Singleton
open class ICJVServiceImpl : ICJVService {

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

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun createICJV(request: ParentICJVRequest): String {
        validateCreateRequest(request)

//        val parentJvNumber = getJvNumber()
        val parentJvNumber = "JV/2223/10"
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

//            if (it.entityId == null) {
//                val data = railsClient.getCogoEntity(it.entityCode.toString())
//                if (data.list.isNotEmpty()) {
//                    it.entityId = UUID.fromString(data.list[0]["id"].toString())
//                }
//            }
//
//            if (it.tradePartyName == null) {
//                val data = railsClient.getListOrganizationTradePartyDetails(it.tradePartyId)
//                if (data.list.isNotEmpty()) {
//                    it.tradePartyName = data.list[0]["legal_business_name"].toString()
//                }
//            }
            it.tradePartyName = "Shayan"
            it.entityId = UUID.fromString("6ac9148e-d626-4a3c-a3e3-53025f1fc253")
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
        val jvList = journalVoucherConverter.convertToModelResponse(documentEntity)
        return jvList
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

        val clientRequest = CreateIncidentRequest(
            type = IncidentType.INTER_COMPANY_JOURNAL_VOUCHER_APPROVAL,
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

        val childJvData = journalVoucherRepository.getJournalVoucherByParentJVId(parentJvId)

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
}
