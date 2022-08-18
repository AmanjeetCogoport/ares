package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.enums.SequenceSuffix
import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.service.implementation.AccountUtilizationServiceImpl
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.mapper.JournalVoucherMapper
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.settlement.JournalVoucherResponse
import com.cogoport.ares.model.settlement.enums.JVCategory
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.request.JournalVoucher
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.hades.client.HadesClient
import com.cogoport.hades.model.incident.IncidentData
import com.cogoport.hades.model.incident.Organization
import com.cogoport.hades.model.incident.enums.IncidentType
import com.cogoport.hades.model.incident.request.CreateIncidentRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import javax.transaction.Transactional

@Singleton
open class JournalVoucherServiceImpl : JournalVoucherService {

    @Inject
    lateinit var journalVoucherConverter: JournalVoucherMapper

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject
    lateinit var accountUtilizationServiceImpl: AccountUtilizationServiceImpl

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
    override suspend fun createJournalVouchers(request: JournalVoucher): String {
        validateCreateRequest(request)
        // create Journal Voucher
        val jv = convertToJournalVoucherEntity(request)
        jv.jvNum = getJvNumber()
        val res = createJV(jv)
        // Send to Incident Management
        val incidentRequest = convertToIncidentModel(request)
        sendToIncidentManagement(request, incidentRequest)

        return res
        // TODO( "Have to decide on adding JV in account utilization" )
//        val accType = getAccountType(request.category, request.type)
//        val accountAccUtilizationRequest = AccUtilizationRequest(
//            documentNo = jvObj.id!!,
//            entityCode = jvObj.entityCode!!,
//            orgSerialId = 1,
//            sageOrganizationId = null,
//            organizationId =  jvObj.tradePartyId,
//            taggedOrganizationId = null,
//            tradePartyMappingId = null,
//            organizationName = jvObj.tradePartnerName,
//            accType = accType,
//            accMode = AccMode.AR,
//            signFlag = -1,
//            currency = jvObj.currency!!,
//            ledCurrency = jvObj.ledCurrency,
//            currencyAmount = jvObj.amount,
//            ledgerAmount = jvObj.amount?.multiply(jvObj.exchangeRate!!),
//            currencyPayment = BigDecimal.ZERO,
//            ledgerPayment = BigDecimal.ZERO,
//            taxableAmount = BigDecimal.ZERO,
//            zoneCode = ,
//            docStatus = DocumentStatus.PROFORMA,
//            docValue = jvObj.jvNum,
//            dueDate = jvObj.validityDate,
//            transactionDate = jvObj.validityDate,
//            serviceType = ServiceType.NA,
//            category = null,
//            createdAt = Timestamp.from(Instant.now()),
//            updatedAt = Timestamp.from(Instant.now()),
//            performedBy = jvObj.createdBy,
//            performedByType = null
//        )
//        accountUtilizationServiceImpl.add(accountAccUtilizationRequest)

//        return journalVoucherConverter.convertEntityToRequest(jv)
    }

    /**
     * Get JV number from generator in fixed format
     * @return: String
     */
    private suspend fun getJvNumber() =
        SequenceSuffix.JV.prefix + "/" + Utilities.getFinancialYear() + "/" + sequenceGeneratorImpl.getPaymentNumber(
            SequenceSuffix.JV.prefix
        )

    private suspend fun createJV(jv: com.cogoport.ares.api.settlement.entity.JournalVoucher): String {
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
        return jvObj.id.toString()
    }

    private fun validateCreateRequest(request: JournalVoucher) {
        if (request.createdBy == null) throw AresException(AresError.ERR_1003, "Created By")
    }

    private fun convertToJournalVoucherEntity(request: JournalVoucher): com.cogoport.ares.api.settlement.entity.JournalVoucher {
        request.status = JVStatus.PENDING
        val jv = journalVoucherConverter.convertRequestToEntity(request)
        jv.createdAt = Timestamp.from(Instant.now())
        jv.updatedAt = Timestamp.from(Instant.now())
        return jv
    }

    private suspend fun sendToIncidentManagement(
        request: JournalVoucher,
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

    private fun convertToIncidentModel(request: JournalVoucher): com.cogoport.hades.model.incident.JournalVoucher {
        request.status = JVStatus.PENDING
        val data = journalVoucherConverter.convertToIncidentModel(request)
        data.createdAt = Timestamp.from(Instant.now())
        data.updatedAt = Timestamp.from(Instant.now())
        return data
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
