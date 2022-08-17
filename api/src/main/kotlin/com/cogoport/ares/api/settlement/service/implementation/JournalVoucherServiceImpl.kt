package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.models.ResponseList
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.service.implementation.AccountUtilizationServiceImpl
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
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class JournalVoucherServiceImpl : JournalVoucherService {

    @Inject
    lateinit var journalVoucherConverter: JournalVoucherMapper

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject
    lateinit var accountUtilizationServiceImpl: AccountUtilizationServiceImpl

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
    override suspend fun createJournalVouchers(request: JournalVoucher): JournalVoucher {

        val jv = journalVoucherConverter.convertRequestToEntity(request)
        jv.status = JVStatus.PENDING
        journalVoucherRepository.save(jv)
        // TODO( "Have to decide on adding JV in account utilization" )
//        val accType = getAccountType(request.category, request.type)
//        val accountAccUtilizationRequest = AccUtilizationRequest(
//            documentNo = jv.id!!,
//            entityCode = jv.entityCode,
//            orgSerialId = 1,
//            sageOrganizationId = null,
//            organizationId =  jv.organizationId,
//            taggedOrganizationId = null,
//            tradePartyMappingId = null,
//            organizationName = jv.organizationName,
//            accType = accType,
//            accMode = AccMode.AR,
//            signFlag = -1,
//            currency = jv.currency,
//            ledCurrency = jv.ledCurrency,
//            currencyAmount = jv.amount,
//            ledgerAmount = jv.amount*jv.exchangeRate,
//            currencyPayment = BigDecimal.ZERO,
//            ledgerPayment = BigDecimal.ZERO,
//            taxableAmount = BigDecimal.ZERO,
//            zoneCode = "WEST",
//            docStatus = DocumentStatus.FINAL,
//            docValue = jv.jvNum,
//            dueDate = jv.validityDate,
//            transactionDate = jv.validityDate,
//            serviceType = ServiceType.NA,
//            category = null,
//            createdAt = Timestamp.from(Instant.now()),
//            updatedAt = Timestamp.from(Instant.now()),
//            performedBy = jv.createdBy,
//            performedByType = null
//        )
//        accountUtilizationServiceImpl.add(accountAccUtilizationRequest)
        return journalVoucherConverter.convertEntityToRequest(jv)
    }

    /**
     * Return Account Type on the basis of jv category and type
     * @param: jvCategory
     * @param: type
     * @return: AccountType
     */
    private fun getAccountType(jvCategory: JVCategory, type: String): AccountType {
        return when(type){
            "CREDIT" ->{
                getCreditAccountType(jvCategory)
            }
            "DEBIT" ->{
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
        return when(jvCategory){
            JVCategory.EXCH ->{
                AccountType.CEXCH
            }
            JVCategory.WOFF ->{
                AccountType.CWOFF
            }
            JVCategory.ROFF ->{
                AccountType.CROFF
            }
            JVCategory.NOSTRO ->{
                AccountType.NOSTRO
            }
            JVCategory.OUTST ->{
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
        return when(jvCategory){
            JVCategory.EXCH ->{
                AccountType.DEXCH
            }
            JVCategory.WOFF ->{
                AccountType.DWOFF
            }
            JVCategory.ROFF ->{
                AccountType.DROFF
            }
            JVCategory.NOSTRO ->{
                AccountType.NOSTRO
            }
            JVCategory.OUTST ->{
                AccountType.OUTST
            }
            else -> {
                throw AresException(AresError.ERR_1009, "JV Category")
            }
        }
    }
}
