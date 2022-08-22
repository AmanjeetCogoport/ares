package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.request.JournalVoucherApproval
import com.cogoport.ares.model.settlement.request.JournalVoucherRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.hades.client.HadesClient
import com.cogoport.hades.model.incident.IncidentData
import com.cogoport.hades.model.incident.Organization
import com.cogoport.hades.model.incident.enums.IncidentType
import com.cogoport.hades.model.incident.request.CreateIncidentRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.SQLException
import java.sql.Timestamp
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

    override suspend fun getJournalVouchers(jvListRequest: JvListRequest): ResponseList<JournalVoucherResponse> {

        val documentEntity = journalVoucherRepository.getListVouchers(
            jvListRequest.entityCode,
            jvListRequest.startDate,
            jvListRequest.endDate,
            jvListRequest.page,
            jvListRequest.pageLimit,
            jvListRequest.status,
            jvListRequest.query
        )
        val totalRecords =
            journalVoucherRepository.countDocument(
                jvListRequest.entityCode,
                jvListRequest.startDate,
                jvListRequest.endDate
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
        validateCreateRequest(request)
        // create Journal Voucher
        val jv = convertToJournalVoucherEntity(request)
        jv.jvNum = getJvNumber()
        val jvEntity = createJV(jv)
        // Send to Incident Management
        val incidentRequestModel = journalVoucherConverter.convertToIncidentModel(jvEntity)
        sendToIncidentManagement(request, incidentRequestModel)

        return jvEntity.id.toString()
        // TODO( "Have to decide on adding JV in account utilization" )
//

//        return journalVoucherConverter.convertEntityToRequest(jv)
    }

    override suspend fun approveJournalVoucher(request: JournalVoucherApproval) {
        val jvEntity = updateJournalVoucher(request)
        val accType = getAccountType(jvEntity.category, jvEntity.type!!)
//        createJvAccUtil(jvEntity, accType)
    }

    override suspend fun rejectJournalVoucher(id: Long, performedBy: UUID?) {
        journalVoucherRepository.updateStatus(id, JVStatus.REJECTED, performedBy)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.JOURNAL_VOUCHERS,
                objectId = id,
                actionName = AresConstants.UPDATE,
                data = mapOf("id" to id, "status" to JVStatus.REJECTED),
                performedBy = performedBy.toString(),
                performedByUserType = null
            )
        )
    }

    private suspend fun updateJournalVoucher(jvObj: JournalVoucherApproval): JournalVoucher {
        val jvEntity = journalVoucherConverter.convertIncidentModelToEntity(jvObj)
        jvEntity.status = JVStatus.APPROVED
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

    private suspend fun createJvAccUtil(
        request: JournalVoucher,
        accType: AccountType
    ) {
        val accountAccUtilizationRequest = AccountUtilization(
            id = null,
            documentNo = request.id!!,
            entityCode = request.entityCode!!,
            orgSerialId = 1,
            sageOrganizationId = null,
            organizationId = request.tradePartyId,
            taggedOrganizationId = null,
            tradePartyMappingId = null,
            organizationName = request.tradePartnerName,
            accType = accType,
            accMode = AccMode.AR,
            signFlag = -1,
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
            accCode = AresModelConstants.AR_ACCOUNT_CODE
        )
        val accUtilObj = accountUtilizationRepository.save(accountAccUtilizationRequest)
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
    }

    /**
     * Get JV number from generator in fixed format
     * @return: String
     */
    private suspend fun getJvNumber() =
        SequenceSuffix.JV.prefix + "/" + Utilities.getFinancialYear() + "/" + sequenceGeneratorImpl.getPaymentNumber(
            SequenceSuffix.JV.prefix
        )

    private suspend fun createJV(jv: JournalVoucher): JournalVoucher {
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
        request.status = JVStatus.PENDING
        val jv = journalVoucherConverter.convertRequestToEntity(request)
        jv.createdAt = Timestamp.from(Instant.now())
        jv.updatedAt = Timestamp.from(Instant.now())
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
                    businessName = request.tradePartnerName
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
        val res = hadesClient.createIncident(clientRequest)
    }

    /**
     * Return Account Type on the basis of jv category and type
     * @param: jvCategory
     * @param: type
     * @return: AccountType
     */
    private fun getAccountType(jvCategory: JVCategory, type: String): AccountType {
        return when (type) {
            "CREDIT" -> {
                getCreditAccountType(jvCategory)
            }
            "DEBIT" -> {
                getDebitAccountType(jvCategory)
            }
            else -> {
                throw AresException(AresError.ERR_1009, "JV type")
            }
        }
    }

    /**
     * Return credit Account Type on the basis of jv category
     * @param: jvCategory
     * @return: AccountType
     */
    private fun getCreditAccountType(jvCategory: JVCategory): AccountType {
        return when (jvCategory) {
            JVCategory.EXCH -> {
                AccountType.CEXCH
            }
            JVCategory.WOFF -> {
                AccountType.CWOFF
            }
            JVCategory.ROFF -> {
                AccountType.CROFF
            }
            JVCategory.NOSTRO -> {
                AccountType.NOSTRO
            }
            JVCategory.OUTST -> {
                AccountType.OUTST
            }
            else -> {
                throw AresException(AresError.ERR_1009, "JV Category")
            }
        }
    }

    /**
     * Return debit Account Type on the basis of jv category
     * @param: jvCategory
     * @return: AccountType
     */
    private fun getDebitAccountType(jvCategory: JVCategory): AccountType {
        return when (jvCategory) {
            JVCategory.EXCH -> {
                AccountType.DEXCH
            }
            JVCategory.WOFF -> {
                AccountType.DWOFF
            }
            JVCategory.ROFF -> {
                AccountType.DROFF
            }
            JVCategory.NOSTRO -> {
                AccountType.NOSTRO
            }
            JVCategory.OUTST -> {
                AccountType.OUTST
            }
            else -> {
                throw AresException(AresError.ERR_1009, "JV Category")
            }
        }
    }
}
