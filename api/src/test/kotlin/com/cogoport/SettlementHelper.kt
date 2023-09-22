package com.cogoport

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.TdsDataResponse
import com.cogoport.ares.api.common.models.TdsDataResponseList
import com.cogoport.ares.api.common.models.TdsStylesResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.settlement.entity.JVAdditionalDetails
import com.cogoport.ares.api.settlement.entity.JournalVoucher
import com.cogoport.ares.api.settlement.entity.ParentJournalVoucher
import com.cogoport.ares.api.settlement.entity.Settlement
import com.cogoport.ares.api.settlement.entity.SettlementListDoc
import com.cogoport.ares.api.settlement.repository.JournalVoucherRepository
import com.cogoport.ares.api.settlement.repository.ParentJVRepository
import com.cogoport.ares.api.settlement.repository.SettlementRepository
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.settlement.Document
import com.cogoport.ares.model.settlement.OrgSummaryResponse
import com.cogoport.ares.model.settlement.SettledInvoice
import com.cogoport.ares.model.settlement.SettlementInvoiceResponse
import com.cogoport.ares.model.settlement.SettlementType
import com.cogoport.ares.model.settlement.SummaryResponse
import com.cogoport.ares.model.settlement.TdsStyle
import com.cogoport.ares.model.settlement.enums.JVStatus
import com.cogoport.ares.model.settlement.enums.SettlementStatus
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.kuber.model.bills.BillDocResponse
import com.cogoport.plutus.model.invoice.response.InvoiceAdditionalResponseV2
import com.cogoport.plutus.model.receivables.SidResponse
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.SimpleDateFormat
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
    ): AccountUtilization {
        val accUtilDoc = AccountUtilization(
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
            transactionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-08-08 05:30:00"),
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
            tdsAmountLoc = tdsAmountLoc,
            settlementEnabled = true,
            updatedAt = Timestamp(SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-08-08 05:30:00").time)
        )

        return accountUtilizationRepo.save(accUtilDoc)
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
        signFlag: Short,
        settlementDate: Date,
    ): Settlement {
        val settlementRecord = Settlement(
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
            settlementDate = settlementDate,
            settlementNum = settlementNum,
            signFlag = signFlag,
            settlementStatus = SettlementStatus.CREATED,
            updatedAt = Timestamp.from(Instant.now()),
            updatedBy = AresConstants.ARES_USER_ID
        )

        return settlementRepository.save(settlementRecord)
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
    ): Payment {
        val payment = Payment(
            id = null,
            accCode = accCode,
            accMode = accMode,
            amount = amount,
            ledAmount = ledAmount,
            bankId = UUID.fromString("d646dc1c-f366-453c-b56f-2788f36c4136"),
            organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
            organizationName = "Inext Logistics & Supply Chain Private Limited",
            tradePartyMappingId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
            taggedOrganizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
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
            transactionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-08-08 05:30:00"),
            createdAt = Timestamp(SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-08-08 05:30:00").time),
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

        return payment
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
                    utr = parentJvData.description,
                    null,
                    null
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

    fun getSettlementList(req: Settlement): List<SettlementListDoc?> {
        return listOf(
            SettlementListDoc(
                id = req.id.toString(),
                sourceDocumentValue = "REC123456",
                destinationDocumentValue = "SINV123455",
                settlementDate = req.settlementDate,
                amount = BigDecimal(100).setScale(4),
                ledAmount = BigDecimal(100).setScale(4),
                currency = "INR",
                ledCurrency = "INR",
                sourceAccType = AccountType.REC,
                destinationAccType = AccountType.SINV,
                sourceId = req.sourceId!!,
                destinationId = req.destinationId,
                destinationOpenInvoiceAmount = BigDecimal(60.0000).setScale(4),
                destinationInvoiceAmount = BigDecimal(100).setScale(4),
            )
        )
    }
    fun getDocumentListResponse(savedRecord: AccountUtilization, isTdsResponse: Boolean): MutableList<Document> {
        return mutableListOf(
            Document(
                id = if (isTdsResponse) savedRecord.id!!.toString() else Hashids.encode(savedRecord.id!!),
                documentNo = "b1bV",
                documentValue = "VIVEK/12234",
                organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
                accountType = "PINV",
                documentType = "Purchase Invoice",
                transactionDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-08-08 05:30:00"),
                mappingId = null,
                dueDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-08-08 05:30:00"),
                documentAmount = BigDecimal(100).setScale(4),
                ledgerAmount = BigDecimal(100).setScale(4),
                ledgerBalance = BigDecimal(60).setScale(4),
                taxableAmount = BigDecimal(0.0000).setScale(4),
                tds = BigDecimal(20.0000).setScale(4),
                tdsPercentage = null,
                afterTdsAmount = BigDecimal(80.0000).setScale(4),
                allocationAmount = if (isTdsResponse) null else BigDecimal(40.0000).setScale(4),
                balanceAfterAllocation = if (isTdsResponse) null else BigDecimal(0.0000),
                settledAmount = BigDecimal(40.0000).setScale(4),
                settledAllocation = if (isTdsResponse) null else BigDecimal(0.0000),
                balanceAmount = (if (isTdsResponse) BigDecimal(40.0000) else BigDecimal(40.0000)).setScale(4),
                currentBalance = BigDecimal(60.0000).setScale(4),
                status = "Partially Paid",
                currency = "INR",
                ledCurrency = "INR",
                settledTds = BigDecimal(0.0000),
                exchangeRate = BigDecimal(1.0000).setScale(20),
                signFlag = -1,
                nostroAmount = BigDecimal(0.0000),
                approved = true,
                accMode = AccMode.AP,
                hasPayrun = false,
                migrated = false
            )
        )
    }

    fun getTdsResponse(): TdsDataResponseList {
        return TdsDataResponseList(
            data = listOf(
                TdsStylesResponse(
                    id = UUID.fromString("3e11a4a6-3e07-4c5d-8ea9-6372b12c7724"),
                    tdsDeductionStyle = "gross",
                    tdsDeductionType = "normal",
                    tdsDeductionRate = BigDecimal(2)
                )
            )
        )
    }

    fun getTdsDataResponse(): TdsDataResponse {
        return TdsDataResponse(
            data = TdsStylesResponse(
                id = UUID.fromString("3e11a4a6-3e07-4c5d-8ea9-6372b12c7724"),
                tdsDeductionStyle = "gross",
                tdsDeductionType = "normal",
                tdsDeductionRate = BigDecimal(2)
            )
        )
    }
    fun getBillResponse(): com.cogoport.kuber.model.common.ResponseList<BillDocResponse> {
        return com.cogoport.kuber.model.common.ResponseList()
    }

    fun getAccountBalance(): SummaryResponse {
        return SummaryResponse(
            openInvoiceAmount = BigDecimal(-60.0000).setScale(4),
            onAccountAmount = BigDecimal(0),
            outstandingAmount = BigDecimal(-60.0000).setScale(4),
            ledgerCurrency = "INR"
        )
    }

    fun getSidResponse(): List<SidResponse> {
        return listOf(
            SidResponse(
                invoiceId = 123343,
                jobNumber = "VIVE134",
                shipmentType = "LOGISTICS",
                pdfUrl = "https://google.com"
            )
        )
    }

    fun getInvoiceAdditionalResponse(): List<InvoiceAdditionalResponseV2> {
        return listOf(
            InvoiceAdditionalResponseV2(
                id = 123,
                invoiceId = 123455,
                value = "irn_number_1222",
                key = "irnNumber"
            )
        )
    }

    fun getInvoiceResponse(req: Settlement, destinationDocument: AccountUtilization): List<SettlementInvoiceResponse> {
        return listOf(
            SettlementInvoiceResponse(
                id = destinationDocument.id,
                invoiceNo = req.destinationId,
                invoiceValue = destinationDocument.documentValue!!,
                invoiceDate = destinationDocument.transactionDate!!,
                invoiceAmount = BigDecimal(100).setScale(4),
                taxableAmount = BigDecimal(0).setScale(4),
                tds = BigDecimal(0).setScale(4),
                afterTdsAmount = BigDecimal(100).setScale(4),
                settledAmount = BigDecimal(40).setScale(4),
                balanceAmount = BigDecimal(60).setScale(4),
                status = "Partially Paid",
                invoiceStatus = "FINAL",
                currency = "INR",
                sid = null,
                shipmentType = null,
                pdfUrl = null,
                tdsPercentage = BigDecimal(2),
                settledTds = BigDecimal(0),
                dueDate = destinationDocument.transactionDate!!
            )
        )
    }

    fun getHistoryDocumentResponse(settlementData: Settlement, sourceDocument: AccountUtilization): List<com.cogoport.ares.model.settlement.HistoryDocument> {
        return listOf(
            com.cogoport.ares.model.settlement.HistoryDocument(
                id = Hashids.encode(sourceDocument.id!!),
                documentNo = Hashids.encode(settlementData.sourceId!!),
                documentValue = "REC123456",
                accountType = "REC",
                currency = "INR",
                balanceAmount = BigDecimal.valueOf(60.0000).setScale(4),
                documentAmount = BigDecimal.valueOf(100.0000).setScale(4),
                ledCurrency = "INR",
                ledgerAmount = BigDecimal.valueOf(100.0000).setScale(4),
                tds = BigDecimal.ZERO,
                afterTdsAmount = BigDecimal.valueOf(100.0000).setScale(4),
                allocationAmount = BigDecimal.valueOf(40.0000).setScale(4),
                balanceAfterAllocation = BigDecimal.ZERO,
                settledAllocation = BigDecimal.ZERO,
                taxableAmount = BigDecimal(0).setScale(4),
                settledTds = BigDecimal.ZERO,
                transactionDate = Date(1691452800000),
                signFlag = -1,
                exchangeRate = BigDecimal.valueOf(1.00000000000000000000).setScale(20),
                settledAmount = BigDecimal.valueOf(40.0000).setScale(4),
                lastEditedDate = Date(1691452800000),
                status = "",
                accMode = AccMode.AR,
                supportingDocUrl = null,
                notPostedSettlementIds = mutableListOf(settlementData.id!!)
            )
        )
    }

    fun getSettledInvoiceResponse(settlementData: Settlement, destinationDocument: AccountUtilization?): List<SettledInvoice> {
        return listOf(
            SettledInvoice(
                id = Hashids.encode(destinationDocument?.id!!),
                organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
                documentNo = Hashids.encode(settlementData.destinationId),
                documentValue = destinationDocument.documentValue!!,
                documentType = SettlementType.SINV,
                accountType = "SINV",
                currency = "INR",
                paymentCurrency = "INR",
                balanceAmount = 60.0000.toBigDecimal().setScale(4),
                currentBalance = 60.0000.toBigDecimal().setScale(4),
                documentAmount = 100.0000.toBigDecimal().setScale(4),
                ledCurrency = "INR",
                ledgerAmount = 100.0000.toBigDecimal().setScale(4),
                taxableAmount = 0.0000.toBigDecimal().setScale(4),
                tds = 0.toBigDecimal(),
                afterTdsAmount = 100.0000.toBigDecimal().setScale(4),
                allocationAmount = 100.0000.toBigDecimal().setScale(4),
                balanceAfterAllocation = 0.toBigDecimal(),
                settledAmount = 100.0000.toBigDecimal().setScale(4),
                settledAllocation = 0.toBigDecimal(),
                transactionDate = settlementData.settlementDate,
                status = "Knocked Off",
                settledTds = 0.toBigDecimal(),
                exchangeRate = 1.0.toBigDecimal().setScale(20),
                signFlag = -1,
                sid = null,
                nostroAmount = 0.toBigDecimal(),
                accMode = AccMode.AR,
                settlementStatus = SettlementStatus.CREATED
            )
        )
    }

    fun getOrgResponse(): OrgSummaryResponse {
        return OrgSummaryResponse(
            orgId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
            orgName = "my_company",
            outstanding = BigDecimal(-120.0000).setScale(4),
            currency = "INR",
            tdsStyle = TdsStyle(style = "gross", rate = BigDecimal(2), type = "normal")
        )
    }
}
