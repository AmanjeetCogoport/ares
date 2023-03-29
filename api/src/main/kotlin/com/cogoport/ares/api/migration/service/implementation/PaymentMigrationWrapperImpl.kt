package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.migration.model.InvoiceDetails
import com.cogoport.ares.api.migration.model.PayLocUpdateRequest
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.api.migration.service.interfaces.SageService
import com.cogoport.ares.api.utils.logger
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class PaymentMigrationWrapperImpl : PaymentMigrationWrapper {

    @Inject lateinit var sageService: SageService

    @Inject
    lateinit var paymentMigration: PaymentMigration

    @Inject
    lateinit var aresMessagePublisher: AresMessagePublisher

    override suspend fun migratePaymentsFromSage(startDate: String?, endDate: String?, bpr: String, mode: String): Int {
        val paymentRecords = sageService.getPaymentDataFromSage(startDate, endDate, bpr, mode)
        logger().info("Total number of payment record to process : ${paymentRecords.size}")
        for (paymentRecord in paymentRecords) {
            aresMessagePublisher.emitPaymentMigration(paymentRecord)
        }
        return paymentRecords.size
    }

    override suspend fun migrateJournalVoucher(startDate: String?, endDate: String?, jvNums: List<String>?): Int {
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
        val jvRecords = sageService.getJournalVoucherFromSage(startDate, endDate, jvNumAsString)
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
            val payLocRecord = getPayLocRecord(paymentRecord)
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
            val payLocRecord = getPayLocRecord(paymentRecord)
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
            val payLocRecord = getPayLocRecordForInvoice(invoiceDetail)
            aresMessagePublisher.emitUtilizationUpdateRecord(payLocRecord)
        }
        return invoiceDetails.size
    }

    override suspend fun updateUtilizationForBill(startDate: String?, endDate: String?, updatedAt: String?): Int {
        val billDetails = sageService.getBillPayLocDetails(startDate, endDate, updatedAt)
        for (billDetail in billDetails) {
            val payLocRecord = getPayLocRecordForInvoice(billDetail)
            aresMessagePublisher.emitUtilizationUpdateRecord(payLocRecord)
        }
        return billDetails.size
    }

    override suspend fun migrateJournalVoucherRecordNew(
        startDate: String?,
        endDate: String?,
        jvNums: List<String>?
    ): Int {
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
        val jvParentRecords = sageService.getJVDetails(startDate, endDate, jvNumAsString)
        jvParentRecords.forEach {
            aresMessagePublisher.emitJournalVoucherMigration(it)
        }
        return jvParentRecords.size
    }

    private fun getPayLocRecord(paymentRecord: PaymentRecord): PayLocUpdateRequest {
        return PayLocUpdateRequest(
            sageOrganizationId = paymentRecord.sageOrganizationId,
            documentValue = paymentRecord.sageRefNumber,
            amtLoc = paymentRecord.accountUtilAmtLed,
            payCurr = paymentRecord.accountUtilPayCurr,
            payLoc = paymentRecord.accountUtilPayLed,
            accMode = paymentRecord.accMode
        )
    }
    private fun getPayLocRecordForInvoice(invoiceDetails: InvoiceDetails): PayLocUpdateRequest {
        return PayLocUpdateRequest(
            sageOrganizationId = invoiceDetails.sageOrganizationId,
            documentValue = invoiceDetails.invoiceNumber,
            amtLoc = invoiceDetails.ledgerTotal,
            payCurr = invoiceDetails.currencyAmountPaid,
            payLoc = invoiceDetails.ledgerAmountPaid,
            accMode = invoiceDetails.accMode
        )
    }
}
