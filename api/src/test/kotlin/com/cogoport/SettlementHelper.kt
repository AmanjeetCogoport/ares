package com.cogoport

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.settlement.entity.JVAdditionalDetails
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.ParentJVRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.enums.SettlementStatus
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

@Singleton
class SettlementHelper(
    private var accountUtilizationRepo: AccountUtilizationRepo,
    private var settlementRepository: SettlementRepository,
    private var paymentRepo: PaymentRepository,
    private var parentJvRepo: ParentJVRepository,
    private var journalVoucherRepository: JournalVoucherRepository
) {
    suspend fun saveAccountUtilizations(
        accMode: AccMode,
        accType: AccountType,
        accCode: Int,
        documentValue: String,
        documentNo: Long,
        signFlag: Short,
        documentStatus: DocumentStatus,
        entityCode: Int,
        payCurr: BigDecimal,
        payLoc: BigDecimal,
        currency: String,
        ledCurrency: String,
        tdsAmount: BigDecimal?,
        tdsAmountLoc: BigDecimal?,
        amountCurr: BigDecimal,
        amountLoc: BigDecimal,
    ): Long? {
        var accUtilDoc = AccountUtilization(
            id = null,
            documentNo = documentNo,
            documentValue = documentValue,
            accMode = accMode,
            accCode = accCode,
            accType = accType,
            signFlag = signFlag,
            amountCurr = amountCurr,
            amountLoc = amountLoc,
            category = "ASSET",
            documentStatus = documentStatus,
            dueDate = Date(),
            entityCode = entityCode,
            transactionDate = Date(),
            migrated = false,
            organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
            organizationName = "my_company",
            orgSerialId = 122122,
            sageOrganizationId = "122122",
            serviceType = ServiceType.FCL_FREIGHT.name,
            taggedOrganizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
            tradePartyMappingId = null,
            zoneCode = "NORTH",
            payCurr = payCurr,
            payLoc = payLoc,
            currency = currency,
            ledCurrency = ledCurrency,
            tdsAmount = tdsAmount,
            tdsAmountLoc = tdsAmountLoc
        )

        accUtilDoc = accountUtilizationRepo.save(accUtilDoc)

        return accUtilDoc.id
    }

    suspend fun saveSettlement(
        sourceId: Long,
        destinationId: Long,
        sourceType: SettlementType,
        destinationType: SettlementType,
        amount: BigDecimal,
        ledAmount: BigDecimal,
        currency: String,
        ledCurrency: String,
        settlementNum: String?,
        signFlag: Short
    ): Long? {
        var settlementRecord = Settlement(
            id = null,
            amount = amount,
            ledAmount = ledAmount,
            createdBy = AresConstants.ARES_USER_ID,
            createdAt = Timestamp.from(Instant.now()),
            currency = currency,
            ledCurrency = ledCurrency,
            destinationId = destinationId,
            destinationType = destinationType,
            sourceId = sourceId,
            sourceType = sourceType,
            settlementDate = Date(),
            settlementNum = settlementNum,
            signFlag = signFlag,
            settlementStatus = SettlementStatus.CREATED,
            updatedAt = Timestamp.from(Instant.now()),
            updatedBy = AresConstants.ARES_USER_ID
        )

        settlementRecord = settlementRepository.save(settlementRecord)

        return settlementRecord.id
    }

    suspend fun savePayment(
        accCode: Int,
        accMode: AccMode,
        amount: BigDecimal,
        ledAmount: BigDecimal,
        currency: String,
        ledCurrency: String,
        entityCode: Int,
        exchangeRate: BigDecimal,
        paymentNum: Long,
        paymentNumValue: String,
        signFlag: Short,
        transRefNumber: String,
        paymentCode: PaymentCode
    ): Long? {
        val payment = Payment(
            id = null,
            accCode = accCode,
            accMode = accMode,
            amount = amount,
            ledAmount = ledAmount,
            bankId = UUID.fromString("d646dc1c-f366-453c-b56f-2788f36c4136"),
            organizationId = UUID.fromString("fb193031-64f6-417c-b538-9064d68db2bd"),
            organizationName = "Inext Logistics & Supply Chain Private Limited",
            tradePartyMappingId = UUID.fromString("7fc682ad-b547-4020-b81b-c95d5ee46d16"),
            taggedOrganizationId = UUID.fromString("6b3bb3c6-4604-4485-acfc-d3fd2820383a"),
            createdBy = UUID.fromString("ec306da2-0d52-4cc1-a7b1-d3a6541f1ce8"),
            currency = currency,
            ledCurrency = ledCurrency,
            entityCode = entityCode,
            exchangeRate = exchangeRate,
            bankName = "rbl",
            bankPayAmount = BigDecimal(0),
            cogoAccountNo = "123456789876543",
            migrated = false,
            orgSerialId = 122334,
            paymentCode = paymentCode,
            paymentNumValue = paymentNumValue,
            paymentNum = paymentNum,
            paymentDocumentStatus = PaymentDocumentStatus.APPROVED,
            payMode = PayMode.BANK,
            refAccountNo = "123456789876543",
            refPaymentId = "123456789876543",
            sageOrganizationId = null,
            sageRefNumber = null,
            signFlag = signFlag,
            transRefNumber = transRefNumber,
            transactionDate = Date()
        )

        paymentRepo.save(payment)

        saveAccountUtilizations(
            accCode = payment.accCode,
            accMode = payment.accMode,
            documentStatus = DocumentStatus.FINAL,
            documentNo = payment.paymentNum!!,
            documentValue = payment.paymentNumValue!!,
            accType = AccountType.valueOf(payment.paymentCode?.name!!),
            amountCurr = payment.amount,
            amountLoc = payment.ledAmount!!,
            payLoc = BigDecimal(0),
            payCurr = BigDecimal(0),
            tdsAmountLoc = BigDecimal(0),
            tdsAmount = BigDecimal(0),
            signFlag = payment.signFlag,
            currency = payment.currency,
            ledCurrency = payment.ledCurrency!!,
            entityCode = payment.entityCode
        )

        return payment.id
    }

    suspend fun saveParentJournalVoucher(
        jvNum: String,
        category: String,
        currency: String,
        ledCurrency: String,
        description: String,
        entityCode: Int,
        exchangeRate: BigDecimal? = BigDecimal(1),
        amount: BigDecimal?,
        ledAmount: BigDecimal?
    ): HashMap<String, Any> {
        var parentJvData = ParentJournalVoucher(
            id = null,
            jvNum = jvNum,
            status = JVStatus.APPROVED,
            category = category,
            createdBy = AresConstants.ARES_USER_ID,
            currency = currency,
            ledCurrency = ledCurrency,
            description = description,
            entityCode = entityCode,
            exchangeRate = exchangeRate,
            jvCodeNum = "AXIS",
            transactionDate = Date(),
            updatedAt = Timestamp.from(Instant.now()),
            updatedBy = AresConstants.ARES_USER_ID,
            validityDate = Date()
        )

        parentJvData = parentJvRepo.save(parentJvData)

        val jvLineItems = saveJvLineItems(
            parentJvData,
            amount = amount,
            ledAmount = ledAmount
        )

        return hashMapOf(
            "parentJv" to parentJvData,
            "jvLIneItems" to jvLineItems
        )
    }

    private suspend fun saveJvLineItems(
        parentJvData: ParentJournalVoucher,
        amount: BigDecimal?,
        ledAmount: BigDecimal?,
    ): List<JournalVoucher> {
        val jvLineItemsProps = getJvLineItemsProps()
        val jvLineItemData = jvLineItemsProps.map { lineItem ->
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
                amount = amount,
                ledAmount = ledAmount,
                description = parentJvData.description,
                entityCode = parentJvData.entityCode,
                entityId = UUID.fromString(AresConstants.ENTITY_ID[parentJvData.entityCode]),
                exchangeRate = parentJvData.exchangeRate,
                glCode = lineItem["glCode"].toString(),
                parentJvId = parentJvData.id,
                type = lineItem["type"].toString(),
                signFlag = lineItem["signFlag"]?.toString()?.toShort(),
                status = JVStatus.APPROVED,
                tradePartyId = UUID.fromString("7fc682ad-b547-4020-b81b-c95d5ee46d16"),
                tradePartyName = "VOLTAS LIMITED",
                validityDate = parentJvData.transactionDate,
                migrated = false,
                deletedAt = null,
                additionalDetails = JVAdditionalDetails(
                    utr = parentJvData.description
                )
            )
        }

        val jvLineItems = journalVoucherRepository.saveAll(jvLineItemData)
        jvLineItems.map {
            if (it.accMode != AccMode.OTHER && it.accMode != null) {
                saveAccountUtilizations(
                    accMode = it.accMode!!,
                    accCode = it.glCode?.toInt()!!,
                    accType = AccountType.valueOf(parentJvData.category),
                    amountCurr = it.amount!!,
                    amountLoc = it.ledAmount!!,
                    currency = it.currency!!,
                    ledCurrency = it.ledCurrency,
                    documentNo = it.id!!,
                    documentValue = it.jvNum,
                    documentStatus = DocumentStatus.FINAL,
                    entityCode = it.entityCode!!,
                    payCurr = BigDecimal(0),
                    payLoc = BigDecimal(0),
                    signFlag = it.signFlag!!,
                    tdsAmount = BigDecimal(0),
                    tdsAmountLoc = BigDecimal(0)
                )
            }
        }

        return jvLineItems
    }

    private fun getJvLineItemsProps(): MutableList<HashMap<String, Any?>> {
        return mutableListOf(
            hashMapOf(
                "accMode" to "AP",
                "glCode" to "321000",
                "type" to "DEBIT",
                "signFlag" to 1
            ),
            hashMapOf(
                "accMode" to "VTDS",
                "glCode" to "324003",
                "type" to "CREDIT",
                "signFlag" to -1
            )
        )
    }
}
