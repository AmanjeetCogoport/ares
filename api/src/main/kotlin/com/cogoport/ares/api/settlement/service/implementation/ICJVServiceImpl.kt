package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.repository.JournalVoucherParentRepo
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.service.interfaces.ICJVService
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentJournalVoucherRequest
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant

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

    override suspend fun createJournalVoucher(request: ParentJournalVoucherRequest): String {
        validateCreateRequest(request)

        val parentJvNumber = getJvNumber()

        val parentData = ParentJournalVoucher(
            id = null,
            status = request.status,
            category = request.category,
            jvNum = parentJvNumber,
            createdBy = request.createdBy,
            updatedBy = request.createdBy
        )
        val parentJvData = journalVoucherParentRepo.save(parentData)

        request.list.mapIndexed { index, it ->
            it.jvNum = parentJvNumber + "/L${index + 1}"
            it.parentJvId = parentJvData.id.toString()
            it.status = JVStatus.PENDING
            val jv = convertToJournalVoucherEntity(it)
            val jvEntity = journalVoucherService.createJV(jv)

            it.id = Hashids.encode(jvEntity.id!!)
            if (request.status == JVStatus.PENDING) {
                val formattedDate = SimpleDateFormat(AresConstants.YEAR_DATE_FORMAT).format(it.validityDate)
                val incidentRequestModel = journalVoucherConverter.convertToIncidentModel(it)
                incidentRequestModel.validityDate = Date.valueOf(formattedDate)
//                journalVoucherService.sendToIncidentManagement(it, incidentRequestModel)
            } else {
                val signFlag = getSignFlag(it.accMode, it.type)
                journalVoucherService.createJvAccUtil(jvEntity, it.accMode, signFlag)
            }
        }
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
        jv.createdAt = Timestamp.from(Instant.now())
        jv.updatedAt = Timestamp.from(Instant.now())
        jv.type = request.type.lowercase()
        return jv
    }

    private fun getSignFlag(accMode: AccMode, type: String): Short {
        return when (type) {
            "CREDIT" -> { -1 }
            "DEBIT" -> { 1 }
            else -> {
                throw AresException(AresError.ERR_1009, "JV type")
            }
        }
    }
}
