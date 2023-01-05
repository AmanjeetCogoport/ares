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
import com.cogoport.ares.api.settlement.model.JournalVoucherApproval
import com.cogoport.ares.api.settlement.repository.JournalVoucherParentRepo
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.service.interfaces.ICJVService
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.request.JournalVoucherReject
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentJournalVoucherRequest
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.hades.client.HadesClient
import com.cogoport.hades.model.incident.IncidentData
import com.cogoport.hades.model.incident.enums.IncidentStatus
import com.cogoport.hades.model.incident.enums.IncidentType
import com.cogoport.hades.model.incident.request.CreateIncidentRequest
import com.cogoport.hades.model.incident.request.UpdateIncidentRequest
import com.cogoport.hades.model.incident.response.InterCompanyJournalVoucher
import jakarta.inject.Inject
import jakarta.inject.Singleton
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
    override suspend fun createJournalVoucher(request: ParentJournalVoucherRequest): String {
        validateCreateRequest(request)

        val parentJvNumber = getJvNumber()

        val parentData = ParentJournalVoucher(
            id = null,
            status = JVStatus.PENDING,
            category = JVCategory.ICJV,
            jvNum = parentJvNumber,
            createdBy = request.createdBy,
            updatedBy = request.createdBy
        )
        val parentJvData = journalVoucherParentRepo.save(parentData)
        val incidentModelData = mutableListOf<com.cogoport.hades.model.incident.JournalVoucher>()

        request.list.mapIndexed { index, it ->
            it.jvNum = parentJvNumber + "/L${index + 1}"
            it.parentJvId = parentJvData.id.toString()
            it.status = JVStatus.PENDING
            it.category = JVCategory.ICJV

            if (it.entityId == null) {
                val data = railsClient.getCogoEntity(it.entityCode.toString())
                if (data.list.isNotEmpty()) {
                    it.entityId = UUID.fromString(data.list[0]["id"].toString())
                }
            }

            if (it.tradePartyName == null) {
                val data = railsClient.getListOrganizationTradePartyDetails(it.tradePartyId)
                if (data.list.isNotEmpty()) {
                    it.tradePartyName = data.list[0]["legal_business_name"].toString()
                }
            }

            val jv = convertToJournalVoucherEntity(it)
            val jvEntity = journalVoucherService.createJV(jv)

            it.id = Hashids.encode(jvEntity.id!!)
            val formattedDate = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(it.validityDate)
            val incidentRequestModel = journalVoucherConverter.convertToIncidentModel(it)
            incidentRequestModel.validityDate = Date.valueOf(formattedDate)
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

    override suspend fun getJournalVoucherByParentJVId(parentId: String): List<JournalVoucherResponse> {
        val documentEntity = journalVoucherRepository.getJournalVoucherByParentJVId(Hashids.decode(parentId)[0])

        val jvList = mutableListOf<JournalVoucherResponse>()
        documentEntity.forEach { doc ->
            jvList.add(journalVoucherConverter.convertToModelResponse((doc)))
        }
        return jvList
    }
    private fun validateCreateRequest(request: ParentJournalVoucherRequest) {
        if (request.createdBy == null) throw AresException(AresError.ERR_1003, "Created By")
    }

    private suspend fun getJvNumber() =
        SequenceSuffix.JV.prefix + "/" + Utilities.getFinancialYear() + "/" + sequenceGeneratorImpl.getPaymentNumber(
            SequenceSuffix.JV.prefix
        )

    private fun convertToJournalVoucherEntity(request: JournalVoucherRequest): JournalVoucher {
        val jv = journalVoucherConverter.convertRequestToEntity(request)
        jv.status = JVStatus.PENDING
        jv.type = request.type.lowercase()
        return jv
    }

    private fun getSignFlag(type: String): Short {
        return when (type) {
            "CREDIT" -> { -1 }
            "DEBIT" -> { 1 }
            else -> {
                throw AresException(AresError.ERR_1009, "JV type")
            }
        }
    }

    private suspend fun sendToIncidentManagement(
        parentJvData: ParentJournalVoucher,
        data: MutableList<com.cogoport.hades.model.incident.JournalVoucher>
    ) {
        var totalDebit = 0.toBigDecimal()
        var totalCredit = 0.toBigDecimal()

        data.map { it ->
            if (it.type == "debit") {
                totalDebit += it.amount
            } else {
                totalCredit += it.amount
            }
        }
        val interCompanyJournalVoucherRequest = InterCompanyJournalVoucher(
            status = parentJvData.status.toString(),
            category = parentJvData.category.toString(),
            createdBy = parentJvData.createdBy!!,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            id = parentJvData.id.toString(),
            jvNum = parentJvData.jvNum!!,
            list = data
        )

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
            createdBy = parentJvData.createdBy!!
        )
        hadesClient.createIncident(clientRequest)
    }

    @Transactional(rollbackOn = [SQLException::class, AresException::class, Exception::class])
    override suspend fun approveJournalVoucher(request: JournalVoucherApproval): String {
        val parentJvId = Hashids.decode(request.journalVoucherData!!.id)[0]
        // Update Journal Voucher
        val parentJvData = journalVoucherParentRepo.findById(parentJvId)
        parentJvData?.status = JVStatus.APPROVED
        journalVoucherParentRepo.update(parentJvData!!)

        val childJvData = journalVoucherRepository.getJournalVoucherByParentJVId(parentJvId)

        childJvData.map { it ->
            it.status = JVStatus.APPROVED
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
                    performedBy = it.createdBy.toString(),
                    performedByUserType = null
                )
            )

            // Insert JV in account_utilizations
            val accMode = AccMode.valueOf(request.journalVoucherData.accMode)
            val signFlag = getSignFlag(request.journalVoucherData.type)
            val accUtilEntity = journalVoucherService.createJvAccUtil(it, accMode, signFlag)
        }
        // Update Incident status on incident management
        hadesClient.updateIncident(
            request = UpdateIncidentRequest(
                status = IncidentStatus.APPROVED,
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
        val parentJvId = Hashids.decode(request.journalVoucherId!!)[0]
        val parentJvData = journalVoucherParentRepo.findById(parentJvId)

        parentJvData?.status = JVStatus.REJECTED
        journalVoucherParentRepo.update(parentJvData!!)

        val childJvData = journalVoucherRepository.getJournalVoucherByParentJVId(parentJvId)

        childJvData.map { it ->
            journalVoucherRepository.reject(it.id!!, request.performedBy!!, request.remark)
            auditService.createAudit(
                AuditRequest(
                    objectType = AresConstants.JOURNAL_VOUCHERS,
                    objectId = it.id!!,
                    actionName = AresConstants.UPDATE,
                    data = mapOf("id" to it.id!!, "status" to JVStatus.REJECTED),
                    performedBy = request.performedBy.toString(),
                    performedByUserType = null
                )
            )
        }

        hadesClient.updateIncident(
            request = UpdateIncidentRequest(
                status = IncidentStatus.REJECTED,
                data = null,
                remark = request.remark,
                updatedBy = request.performedBy!!
            ),
            id = request.incidentId
        )
        return request.incidentId!!
    }
}
