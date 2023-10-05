package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.events.PlutusMessagePublisher
import com.cogoport.ares.api.migration.constants.MigrationRecordType
import com.cogoport.ares.api.migration.model.InvoiceDetails
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.api.migration.service.interfaces.SageService
import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.api.payment.service.implementation.SequenceGeneratorImpl
import com.cogoport.ares.api.settlement.repository.AccountClassRepository
import com.cogoport.ares.api.settlement.repository.GlCodeMasterRepository
import com.cogoport.ares.api.utils.Utilities
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.common.PaymentStatusSyncMigrationReq
import com.cogoport.ares.model.common.TdsAmountReq
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.SagePaymentNumMigrationResponse
import com.cogoport.ares.model.settlement.GlCodeMaster
import com.cogoport.ares.model.settlement.event.InvoiceBalance
import com.cogoport.ares.model.settlement.event.UpdateInvoiceBalanceEvent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import javax.transaction.Transactional

@Singleton
open class PaymentMigrationWrapperImpl(
    private var paymentRepository: PaymentRepository,
    private var sequenceGeneratorImpl: SequenceGeneratorImpl,
    private var plutusMessagePublisher: PlutusMessagePublisher
) : PaymentMigrationWrapper {

    @Inject lateinit var sageService: SageService

    @Inject
    lateinit var aresMessagePublisher: AresMessagePublisher

    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    @Inject
    lateinit var accountUtilizationRepo: AccountUtilizationRepo

    @Inject
    lateinit var glCodeMasterRepository: GlCodeMasterRepository

    @Inject lateinit var accountClassRepo: AccountClassRepository

    private var accountUtilizationUpdateCount: Int = 0

    override suspend fun migratePaymentsFromSage(startDate: String?, endDate: String?, bpr: String, mode: String): Int {
        val paymentRecords = sageService.getPaymentDataFromSage(startDate, endDate, bpr, mode)
        logger().info("Total number of payment record to process : ${paymentRecords.size}")
        for (paymentRecord in paymentRecords) {
            aresMessagePublisher.emitPaymentMigration(paymentRecord)
        }
        return paymentRecords.size
    }

    override suspend fun migrateJournalVoucher(startDate: String?, endDate: String?, jvNums: List<String>?): Int {
        var jvNumAsString: String? = null
        if (jvNums != null) {
            jvNumAsString = getFormattedJVNums(jvNums)
        }
        val jvRecords = sageService.getJournalVoucherFromSageCorrected(startDate, endDate, jvNumAsString, null)
//        val jvRecords = sageService.getJournalVoucherFromSage(startDate, endDate, jvNumAsString)
        logger().info("Total number of journal voucher record to process : ${jvRecords.size}")
//        for (jvRecord in jvRecords) {
//            aresMessagePublisher.emitJournalVoucherMigration(jvRecord)
//        }
        return jvRecords.size
    }

    override suspend fun migratePaymentsByDate(startDate: String?, endDate: String?): Int {
        val paymentRecords = sageService.migratePaymentsByDate(startDate, endDate, null)
        logger().info("Total number of payment record to process : ${paymentRecords.size}")
        for (paymentRecord in paymentRecords) {
            aresMessagePublisher.emitPaymentMigration(paymentRecord)
        }
        return paymentRecords.size
    }

    override suspend fun migratePaymentsByPaymentNum(paymentNums: List<String>): Int {
        val payments = StringBuilder()
        for (paymentNum in paymentNums) {
            payments.append("'")
            payments.append(paymentNum)
            payments.append("',")
        }
        val paymentRecords = sageService.migratePaymentByPaymentNum(payments.substring(0, payments.length - 1).toString())
        logger().info("Total number of payment record to process : ${paymentRecords.size}")
        for (paymentRecord in paymentRecords) {
            aresMessagePublisher.emitPaymentMigration(paymentRecord)
        }
        return paymentRecords.size
    }

    override suspend fun migrateSettlementsWrapper(startDate: String, endDate: String, entries: Map<String, String>?): Int {
        var size = if (entries != null) {
            migrateSettlementEntries(startDate, endDate, entries)
        } else {
            val settlementRecords = sageService.getSettlementDataFromSage(startDate, endDate, null, null)
            logger().info("settlements Records to migrate:${settlementRecords.size}")
            for (settlementRecord in settlementRecords) {
                aresMessagePublisher.emitSettlementRecord(settlementRecord)
            }
            settlementRecords.size
        }
        return size
    }

    private suspend fun migrateSettlementEntries(startDate: String, endDate: String, entries: Map<String, String>): Int {
        for (entry in entries.keys) {
            val destination = entries[entry]
            val settlementRecords = sageService.getSettlementDataFromSage(startDate, endDate, entry, destination)
            for (settlementRecord in settlementRecords) {
                aresMessagePublisher.emitSettlementRecord(settlementRecord)
            }
        }
        return entries.size
    }

    override suspend fun updateUtilizationAmount(startDate: String?, endDate: String?, updatedAt: String?): Int {
        val paymentRecords = sageService.migratePaymentsByDate(startDate, endDate, updatedAt)
        for (paymentRecord in paymentRecords) {
            val payLocRecord = getPayLocRecord(paymentRecord, MigrationRecordType.PAYMENT)
            aresMessagePublisher.emitUtilizationUpdateRecord(payLocRecord)
        }
        return paymentRecords.size
    }

    override suspend fun updateUtilizationAmountByPaymentNum(paymentNums: List<String>): Int {
        val payments = StringBuilder()
        for (paymentNum in paymentNums) {
            payments.append("'")
            payments.append(paymentNum)
            payments.append("',")
        }
        val paymentRecords = sageService.migratePaymentByPaymentNum(payments.substring(0, payments.length - 1).toString())
        for (paymentRecord in paymentRecords) {
            val payLocRecord = getPayLocRecord(paymentRecord, MigrationRecordType.PAYMENT)
            aresMessagePublisher.emitUtilizationUpdateRecord(payLocRecord)
        }
        return paymentRecords.size
    }

    override suspend fun updateUtilizationForInvoice(
        startDate: String?,
        endDate: String?,
        updatedAt: String?,
        invoiceNumbers: List<String>?
    ): Int {
        var invoiceNums = StringBuilder()
        if (invoiceNumbers != null) {
            invoiceNums.append("(")
            for (invoiceNumber in invoiceNumbers) {
                invoiceNums.append("'")
                invoiceNums.append(invoiceNumber)
                invoiceNums.append("',")
            }
        }
        val invoices = invoiceNums.substring(0, invoiceNums.length - 1).toString()
        val invoiceDetails = sageService.getInvoicesPayLocDetails(startDate, endDate, updatedAt, "$invoices)")
        for (invoiceDetail in invoiceDetails) {
            val payLocRecord = getPayLocRecordForInvoice(invoiceDetail, MigrationRecordType.INVOICE)
            aresMessagePublisher.emitUtilizationUpdateRecord(payLocRecord)
        }
        return invoiceDetails.size
    }

    override suspend fun updateUtilizationForBill(startDate: String?, endDate: String?, updatedAt: String?): Int {
        val billDetails = sageService.getBillPayLocDetails(startDate, endDate, updatedAt)
        for (billDetail in billDetails) {
            val payLocRecord = getPayLocRecordForInvoice(billDetail, MigrationRecordType.BILL)
            aresMessagePublisher.emitUtilizationUpdateRecord(payLocRecord)
        }
        return billDetails.size
    }

    override suspend fun migrateJournalVoucherRecordNew(
        startDate: String?,
        endDate: String?,
        jvNums: List<String>?,
        sageJvId: List<String>?
    ): Int {
        var jvNumAsString: String? = null
        var jvIdAsString: String? = null
        if (jvNums != null) {
            jvNumAsString = getFormattedJVNums(jvNums)
        }
        if (sageJvId != null) {
            jvIdAsString = getFormattedJVNums(sageJvId)
        }
        val jvParentRecords = sageService.getJVDetails(startDate, endDate, jvNumAsString, jvIdAsString)
        jvParentRecords.forEach {
            aresMessagePublisher.emitJournalVoucherMigration(it)
        }
        return jvParentRecords.size
    }

    private fun getPayLocRecord(paymentRecord: PaymentRecord, recordType: MigrationRecordType): PayLocUpdateRequest {
        return PayLocUpdateRequest(
            sageOrganizationId = paymentRecord.sageOrganizationId,
            documentValue = paymentRecord.sageRefNumber,
            amtLoc = paymentRecord.accountUtilAmtLed,
            payCurr = paymentRecord.accountUtilPayCurr,
            payLoc = paymentRecord.accountUtilPayLed,
            accMode = paymentRecord.accMode,
            recordType = recordType,
            entityCode = paymentRecord.entityCode
        )
    }
    private fun getPayLocRecordForInvoice(invoiceDetails: InvoiceDetails, recordType: MigrationRecordType): PayLocUpdateRequest {
        return PayLocUpdateRequest(
            sageOrganizationId = invoiceDetails.sageOrganizationId,
            documentValue = invoiceDetails.invoiceNumber,
            amtLoc = invoiceDetails.ledgerTotal,
            payCurr = invoiceDetails.currencyAmountPaid,
            payLoc = invoiceDetails.ledgerAmountPaid,
            accMode = invoiceDetails.accMode,
            recordType = recordType,
            entityCode = invoiceDetails.entityCodeNum?.toInt()
        )
    }

    override suspend fun migrateSettlementNumWrapper(ids: List<Long>) {
        ids.forEach {
            aresMessagePublisher.emitMigrateSettlementNumber(it)
        }
    }

    override suspend fun migrateTdsAmount(req: List<TdsAmountReq>) {
        req.forEach {
            val account = accountUtilizationRepository.findRecord(it.documentNo, null, AccMode.AP.name)
            if (account != null) {
                accountUtilizationRepo.updateTdsAmount(it.documentNo, it.tdsAmount, it.tdsAmountLoc)
            }
        }
    }

    override suspend fun migrateNewPR(startDate: String, endDate: String, bpr: String?, accMode: String) {
        val records = sageService.getNewPeriodRecord(startDate, endDate, bpr, accMode)
        records.forEach {
            aresMessagePublisher.emitNewPeriodRecords(it)
        }
    }

    override suspend fun migrateJVUtilization(startDate: String?, endDate: String?, jvNums: List<String>?): Int {
        var jvNumbersList = java.lang.StringBuilder()
        var jvNumAsString: String? = null
        if (jvNums != null) {
            for (jvNum in jvNums) {
                jvNumbersList.append("'")
                jvNumbersList.append(jvNum)
                jvNumbersList.append("',")
            }
            jvNumAsString = jvNumbersList.substring(0, jvNumbersList.length - 1).toString()
        }
        val records = sageService.getJVDetailsForScheduler(startDate, endDate, jvNumAsString)
        records.forEach {
            aresMessagePublisher.emitJVUtilization(it)
        }
        return records.size
    }

    override suspend fun migrateGlAccount(): Int {
        val glRecords = sageService.getGLCode()
        logger().info("Total number of gl account records to process: ${glRecords.size}")
        for (glRecord in glRecords) {
            val glCode = GlCodeMaster(
                accountCode = glRecord.accountCode,
                description = glRecord.description,
                ledAccount = glRecord.ledAccount,
                accountType = glRecord.accountType,
                classCode = glRecord.classCode,
                createdBy = glRecord.createdBy,
                updatedBy = glRecord.updatedBy,
                createdAt = glRecord.createdAt,
                updatedAt = glRecord.updatedAt,
                accountClassId = null
            )
            aresMessagePublisher.emitUpsertMigrateGlCode(glCode)
        }
        return glRecords.size
    }

    override suspend fun createGLCode(request: GlCodeMaster) {
        val classCodeDetails = accountClassRepo.getAccountClass(request.ledAccount, request.classCode)

        val glAccount = com.cogoport.ares.api.settlement.entity.GlCodeMaster(
            id = null,
            accountCode = request.accountCode,
            description = request.description,
            ledAccount = request.ledAccount,
            accountType = request.accountType,
            classCode = request.classCode,
            accountClassId = classCodeDetails.id!!,
            createdBy = request.createdBy,
            updatedAt = request.updatedAt,
            updatedBy = request.updatedBy,
            createdAt = request.createdAt
        )
        glCodeMasterRepository.save(glAccount)
    }

    override suspend fun upsertMigrateGLCode(request: GlCodeMaster) {
        val classCodeDetails = accountClassRepo.getAccountClass(request.ledAccount, request.classCode)
        var glAccount = com.cogoport.ares.api.settlement.entity.GlCodeMaster(
            id = null,
            accountCode = request.accountCode,
            description = request.description,
            ledAccount = request.ledAccount,
            accountType = request.accountType,
            classCode = request.classCode,
            accountClassId = classCodeDetails.id!!,
            createdBy = request.createdBy,
            updatedAt = request.updatedAt,
            updatedBy = request.updatedBy,
            createdAt = request.createdAt
        )
        val glCodeMaster = glCodeMasterRepository.isPresent(glAccount.accountCode!!, glAccount.ledAccount!!)
        if (glCodeMaster == null)
            glCodeMasterRepository.save(glAccount)
        else
            glAccount.id = glCodeMaster!!
        glCodeMasterRepository.update(glAccount)
    }

    private fun getFormattedJVNums(documentValue: List<String>): String {

        var formattedData = java.lang.StringBuilder()
        for (jvNum in documentValue) {
            formattedData.append("'")
            formattedData.append(jvNum)
            formattedData.append("',")
        }
        return formattedData.substring(0, formattedData.length - 1).toString()
    }

    override suspend fun removeDuplicatePayNums(payNumValues: List<String>): Int {
        payNumValues.forEach { it ->
            val payments = paymentRepository.findByPaymentNumValue(it) ?: return@forEach
            val groupedPayments = payments.groupBy { it.narration }

            groupedPayments.forEach {
                if (it.value.size > 1)
                    updatePaymentValue(it.value)
                else
                    updatePaymentNumAndValue(it.value)
            }
        }
        return accountUtilizationUpdateCount
    }

    @Transactional
    open suspend fun updatePaymentValue(payments: List<Payment>) {
        val tdsPayment = payments.find { it.paymentCode in listOf(PaymentCode.VTDS, PaymentCode.CTDS) }
        val newPayNumValueForTds = tdsPayment?.paymentCode.toString() + tdsPayment?.paymentNumValue?.substring(3)
        if (paymentRepository.countByPaymentNumValueEquals(newPayNumValueForTds) > 0 || tdsPayment == null) {
            updatePaymentNumAndValue(payments)
            return
        }
        tdsPayment.paymentNumValue = newPayNumValueForTds
        paymentRepository.update(tdsPayment)
    }

    @Transactional
    open suspend fun updatePaymentNumAndValue(payments: List<Payment>) {
        val payment = payments.find { it.paymentCode !in listOf(PaymentCode.VTDS, PaymentCode.CTDS) } ?: return
        val newPayNumAndValue = sequenceGeneratorImpl.getPaymentNumAndValue(payment.paymentCode!!, null)
        var amount = payment.amount
        val tdsPayment = payments.find { it.paymentCode in listOf(PaymentCode.VTDS, PaymentCode.CTDS) }
        if (payments.size > 1 && tdsPayment != null) {
            amount += tdsPayment.amount
            val newPayNumAndValueForTds = sequenceGeneratorImpl.getPaymentNumAndValue(tdsPayment.paymentCode!!, newPayNumAndValue.second)
            tdsPayment.paymentNum = newPayNumAndValueForTds.second
            tdsPayment.paymentNumValue = newPayNumAndValueForTds.first
            paymentRepository.update(tdsPayment)
        }
        val count = accountUtilizationRepo.updateAccountUtilizationForPayment(
            payment.paymentNumValue!!,
            amount,
            payment.transactionDate!!,
            payment.organizationId!!,
            payment.taggedOrganizationId!!,
            payment.paymentCode?.name!!,
            newPayNumAndValue.first,
            newPayNumAndValue.second
        )
        accountUtilizationUpdateCount += count
        payment.paymentNum = newPayNumAndValue.second
        payment.paymentNumValue = newPayNumAndValue.first
        paymentRepository.update(payment)
    }

    override suspend fun paymentStatusSyncMigration(paymentStatusSyncMigrationReq: PaymentStatusSyncMigrationReq): Int {
        val accountUtilizations = accountUtilizationRepo.findRecords(
            paymentStatusSyncMigrationReq.documentNumbers!!,
            paymentStatusSyncMigrationReq.accTypes?.map { it.name }!!,
            paymentStatusSyncMigrationReq.accMode?.name
        )
        accountUtilizations.forEach {
            val paymentStatus = Utilities.getPaymentStatus(it)
            plutusMessagePublisher.emitInvoiceBalance(
                invoiceBalanceEvent = UpdateInvoiceBalanceEvent(
                    invoiceBalance = InvoiceBalance(
                        invoiceId = it.documentNo,
                        balanceAmount = paymentStatus.second,
                        performedBy = paymentStatusSyncMigrationReq.performedBy,
                        performedByUserType = paymentStatusSyncMigrationReq.performedByUserType,
                        paymentStatus = paymentStatus.first
                    ),
                    null
                )
            )
        }
        return accountUtilizations.size
    }

    override suspend fun migrateSagePaymentNum(sageRefNumber: List<String>): Int {
        val sagePaymentNum = sageService.getSagePaymentNum(sageRefNumber)

        logger().info("Total number of payment record to process : ${sagePaymentNum.size}")
        for (paymentRecord in sagePaymentNum) {
            val sagePayment = SagePaymentNumMigrationResponse(
                sageRefNum = paymentRecord.sageRefNum,
                sagePaymentNum = paymentRecord.sagePaymentNum
            )
            aresMessagePublisher.emitSagePaymentNumMigration(sagePayment)
        }
        return sagePaymentNum.size
    }

    override suspend fun migrateMTCCVJV(
        startDate: String?,
        endDate: String?
    ): Int {
        val jvParentRecords = sageService.getMTCJVDetails(startDate, endDate)
        jvParentRecords.forEach {
            aresMessagePublisher.emitJournalVoucherMigration(it)
        }
        return jvParentRecords.size
    }

    override suspend fun migrateJournalVoucherRecordTDS(
        startDate: String?,
        endDate: String?,
        jvNums: List<String>?,
        sageJvId: List<String>?
    ): Int {
        var jvNumAsString: String? = null
        var jvIdAsString: String? = null
        if (jvNums != null) {
            jvNumAsString = getFormattedJVNums(jvNums)
        }
        if (sageJvId != null) {
            jvIdAsString = getFormattedJVNums(sageJvId)
        }
        val jvParentRecords = sageService.getTDSJVDetails(startDate, endDate, jvNumAsString, jvIdAsString)
        jvParentRecords.forEach {
            aresMessagePublisher.emitTDSJournalVoucherMigration(it)
        }
        return jvParentRecords.size
    }
}
