package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.OrgIdAndEntityCode
import com.cogoport.ares.api.payment.model.AuditRequest
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.AuditService
import com.cogoport.ares.api.settlement.entity.JVAdditionalDetails
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
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
import com.cogoport.ares.model.settlement.ListOrganizationTradePartyDetailsResponse
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.request.JVBulkFileUploadRequest
import com.cogoport.brahma.hashids.Hashids
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.transaction.Transactional

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

    @Inject
    lateinit var railsClient: RailsClient

    @Transactional
    override suspend fun updateJournalVoucherStatus(id: Long, isUtilized: Boolean, performedBy: UUID, performedByUserType: String?, documentValue: String?) {
        parentJvRepo.updateIsUtilizedColumn(id, isUtilized, performedBy, documentValue)
        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.PARENT_JOURNAL_VOUCHERS,
                objectId = id,
                actionName = AresConstants.UPDATE,
                data = if (isUtilized) mapOf("id" to id, "status" to "UTILIZED") else mapOf("id" to id, "status" to "UNUTILIZED"),
                performedBy = performedBy.toString(),
                performedByUserType = performedByUserType
            )
        )
    }

    override suspend fun createJvAccUtil(request: JournalVoucher, accMode: AccMode, signFlag: Short, settlementEnabled: Boolean): AccountUtilization {
        val organization = railsClient.getListOrganizationTradePartyDetails(request.tradePartyId!!)

        if (organization.list.isEmpty()) {
            throw AresException(AresError.ERR_1530, "")
        }
        val orgSerialId = organization.list[0]["serial_id"]!!.toString().toLong()

        val accCode = when (accMode) {
            AccMode.AR -> AresModelConstants.AR_ACCOUNT_CODE
            AccMode.AP -> AresModelConstants.AP_ACCOUNT_CODE
            AccMode.CSD -> AresModelConstants.CSD_ACCOUNT_CODE
            else -> null
        }

        val accountAccUtilizationRequest = AccountUtilization(
            id = null,
            documentNo = request.id!!,
            entityCode = request.entityCode!!,
            orgSerialId = orgSerialId,
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
            accCode = accCode!!,
            migrated = false,
            settlementEnabled = settlementEnabled,
            isProforma = false
        )
        val accUtilObj = accountUtilizationRepository.save(accountAccUtilizationRequest)

        if (accUtilObj.accMode == AccMode.AR) {
            aresMessagePublisher.emitUpdateCustomerOutstanding(UpdateSupplierOutstandingRequest(accountAccUtilizationRequest.organizationId))
            aresMessagePublisher.emitUpdateCustomerDetail(OrgIdAndEntityCode(accountAccUtilizationRequest.organizationId!!, accountAccUtilizationRequest.entityCode))
        }

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

    override suspend fun createTdsJvLineItems(
        parentJvData: ParentJournalVoucher,
        accountUtilization: AccountUtilization?,
        jvLineItems: MutableList<HashMap<String, Any?>>,
        tdsAmount: BigDecimal?,
        tdsLedAmount: BigDecimal?,
        createdByUserType: String?,
        payCurrTds: BigDecimal?,
        payLocTds: BigDecimal?,
        utr: String?
    ): Long? {
        val jvLineItemData = jvLineItems.map { lineItem ->
            JournalVoucher(
                id = null,
                jvNum = parentJvData.jvNum!!,
                accMode = if (lineItem["accMode"] != null) AccMode.valueOf(lineItem["accMode"]!!.toString()) else AccMode.OTHER,
                category = parentJvData.category,
                createdAt = parentJvData.createdAt,
                createdBy = parentJvData.createdBy,
                updatedAt = parentJvData.createdAt,
                updatedBy = parentJvData.createdBy,
                currency = parentJvData.currency,
                ledCurrency = parentJvData.ledCurrency!!,
                amount = tdsAmount,
                ledAmount = tdsLedAmount,
                description = parentJvData.description,
                entityCode = parentJvData.entityCode,
                entityId = UUID.fromString(AresConstants.ENTITY_ID[parentJvData.entityCode]),
                exchangeRate = parentJvData.exchangeRate,
                glCode = lineItem["glCode"].toString(),
                parentJvId = parentJvData.id,
                type = lineItem["type"].toString(),
                signFlag = lineItem["signFlag"]?.toString()?.toShort(),
                status = JVStatus.APPROVED,
                tradePartyId = accountUtilization?.organizationId,
                tradePartyName = accountUtilization?.organizationName,
                validityDate = parentJvData.transactionDate,
                migrated = false,
                deletedAt = null,
                additionalDetails = JVAdditionalDetails(
                    utr = utr,
                    bpr = null,
                    aresDocumentId = null
                )
            )
        }

        val jvLineItems = journalVoucherRepository.saveAll(jvLineItemData)
        val jvLineItemsWithAccModeAp = jvLineItems.first { it.accMode == AccMode.AP }

        jvLineItems.map {
            createJvAccUtilForTds(it, accountUtilization, createdBy = parentJvData.createdBy, createdByUserType, payCurrTds, payLocTds)
        }

        return jvLineItemsWithAccModeAp.id!!
    }

    private suspend fun createJvAccUtilForTds(
        journalVoucher: JournalVoucher,
        accountUtilization: AccountUtilization?,
        createdBy: UUID?,
        createdByUserType: String?,
        payCurrTds: BigDecimal?,
        payLocTds: BigDecimal?
    ) {
        val accountUtilEntity = AccountUtilization(
            id = null,
            documentNo = journalVoucher.id!!,
            documentValue = journalVoucher.jvNum,
            zoneCode = accountUtilization?.zoneCode.toString(),
            serviceType = accountUtilization?.serviceType!!,
            documentStatus = DocumentStatus.FINAL,
            entityCode = accountUtilization.entityCode,
            category = accountUtilization.category,
            sageOrganizationId = null,
            organizationId = accountUtilization.organizationId!!,
            taggedOrganizationId = accountUtilization.taggedOrganizationId,
            tradePartyMappingId = accountUtilization.tradePartyMappingId,
            organizationName = accountUtilization.organizationName,
            accCode = journalVoucher.glCode?.toInt()!!,
            accType = AccountType.VTDS,
            accMode = journalVoucher.accMode!!,
            signFlag = journalVoucher.signFlag!!,
            currency = journalVoucher.currency!!,
            ledCurrency = journalVoucher.ledCurrency,
            amountCurr = journalVoucher.amount!!,
            amountLoc = journalVoucher.ledAmount!!,
            payCurr = if (journalVoucher.accMode === AccMode.VTDS) BigDecimal.ZERO else payCurrTds!!,
            payLoc = if (journalVoucher.accMode === AccMode.VTDS) BigDecimal.ZERO else payLocTds!!,
            taxableAmount = BigDecimal.ZERO,
            tdsAmountLoc = BigDecimal.ZERO,
            tdsAmount = BigDecimal.ZERO,
            dueDate = accountUtilization.dueDate,
            transactionDate = journalVoucher.validityDate,
            createdAt = Timestamp.from(Instant.now()),
            updatedAt = Timestamp.from(Instant.now()),
            orgSerialId = accountUtilization.orgSerialId,
            migrated = false,
            settlementEnabled = true,
            isProforma = false
        )
        val accUtilObj = accountUtilizationRepository.save(accountUtilEntity)

        auditService.createAudit(
            AuditRequest(
                objectType = AresConstants.ACCOUNT_UTILIZATIONS,
                objectId = accUtilObj.id,
                actionName = AresConstants.CREATE,
                data = accUtilObj,
                performedBy = createdBy.toString(),
                performedByUserType = createdByUserType
            )
        )
    }

    override suspend fun makeJournalVoucherLineItem(
        parentMapping: HashMap<String, ParentJournalVoucher>,
        journalVouchers: List<Map<String, Any>>,
        jvBulkFileUploadRequest: JVBulkFileUploadRequest,
        tradePartyDetails: Map<String, ListOrganizationTradePartyDetailsResponse>,
        documentId: Long?
    ): List<JournalVoucher> {
        val jvList = mutableListOf<JournalVoucher>()
        journalVouchers.groupBy { it["parent_id"].toString() }.map { (k, v) ->
            v.map {
                val tradeParty = if (it["bpr"].toString() != "" && it["acc_mode"].toString() != "") tradePartyDetails[it["bpr"].toString() + it["acc_mode"].toString()]!!.list[0] else null
                val tradePartyId = if (tradeParty == null) null else UUID.fromString(tradeParty["id"].toString())
                val tradePartyName = if (tradeParty == null) null else tradeParty["legal_business_name"].toString()
                val parentJvData = parentMapping[k]
                jvList.add(
                    JournalVoucher(
                        id = null,
                        jvNum = parentJvData?.jvNum!!,
                        accMode = if (it["acc_mode"].toString().isNotBlank()) AccMode.valueOf(it["acc_mode"].toString()) else AccMode.OTHER,
                        category = parentJvData.category,
                        createdAt = parentJvData.createdAt,
                        createdBy = jvBulkFileUploadRequest.performedByUserId,
                        updatedAt = parentJvData.createdAt,
                        updatedBy = jvBulkFileUploadRequest.performedByUserId,
                        currency = parentJvData.currency,
                        ledCurrency = parentJvData.ledCurrency!!,
                        amount = BigDecimal(it["amount"].toString()),
                        ledAmount = BigDecimal(it["amount"].toString()).multiply(parentJvData.exchangeRate),
                        description = parentJvData.description,
                        entityCode = parentJvData.entityCode,
                        entityId = UUID.fromString(AresConstants.ENTITY_ID[parentJvData.entityCode]),
                        exchangeRate = parentJvData.exchangeRate,
                        glCode = it["gl_code"].toString(),
                        parentJvId = parentJvData.id,
                        type = it["type"].toString(),
                        signFlag = getSignFlag(it["type"].toString()),
                        status = JVStatus.APPROVED,
                        tradePartyId = tradePartyId,
                        tradePartyName = tradePartyName,
                        validityDate = parentJvData.transactionDate,
                        migrated = false,
                        deletedAt = null,
                        additionalDetails = JVAdditionalDetails(
                            utr = null,
                            bpr = it["bpr"].toString(),
                            aresDocumentId = documentId
                        )
                    )
                )
            }
        }

        return jvList
    }

    private fun getSignFlag(type: String): Short {
        return when (type.uppercase()) {
            "CREDIT" -> -1
            "DEBIT" -> 1
            else -> throw AresException(AresError.ERR_1009, "JV type")
        }
    }
}
