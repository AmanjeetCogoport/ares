package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.ParentJVRepository
import com.cogoport.ares.api.settlement.service.interfaces.JournalVoucherService
import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.request.UpdateSupplierOutstandingRequest
import com.cogoport.ares.model.settlement.JvLineItemResponse
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Singleton
open class JournalVoucherServiceImpl : JournalVoucherService {

    @Inject
    lateinit var journalVoucherRepository: JournalVoucherRepository

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var auditService: AuditService

    @Inject
    lateinit var aresMessagePublisher: AresMessagePublisher

    @Inject
    lateinit var parentJvRepo: ParentJVRepository

    override suspend fun updateJournalVoucherStatus(id: Long, isUtilized: Boolean, performedBy: UUID, performedByUserType: String?, documentValue: String?) {
        val jvDetails = parentJvRepo.updateIsUtilizedColumn(id, isUtilized, performedBy, documentValue)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PARENT_JOURNAL_VOUCHERS,
                objectId = jvDetails.id,
                actionName = AresConstants.UPDATE,
                data = mapOf("id" to jvDetails.id, "status" to "UTILIZED"),
                performedBy = performedBy.toString(),
                performedByUserType = performedByUserType
            )
        )
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
            accType = AccountType.valueOf(request.category),
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

    override suspend fun getJVLineItems(parentJVId: String): MutableList<JvLineItemResponse> {
        val parentId = Hashids.decode(parentJVId)[0]
        val lineItems = journalVoucherRepository.getJournalVoucherByParentJVId(parentId)

        val jvLineItems = mutableListOf<JvLineItemResponse>()

        lineItems.forEach { lineItem ->
            val jvLineItem = JvLineItemResponse(
                id = Hashids.encode(lineItem.id!!),
                accMode = if (lineItem.accMode == AccMode.OTHER) null else lineItem.accMode,
                tradePartyId = lineItem.tradePartyId,
                tradePartyName = lineItem.tradePartyName,
                amount = lineItem.amount!!,
                ledAmount = lineItem.ledAmount!!,
                entityCode = lineItem.entityCode,
                type = lineItem.type,
                parentId = parentJVId,
                glCode = lineItem.glCode,
                entityId = lineItem.entityId,
                currency = lineItem.currency,
                ledCurrency = lineItem.ledCurrency,
                description = lineItem.description,
                category = lineItem.category
            )
            jvLineItems.add(jvLineItem)
        }

        return jvLineItems
    }
}
