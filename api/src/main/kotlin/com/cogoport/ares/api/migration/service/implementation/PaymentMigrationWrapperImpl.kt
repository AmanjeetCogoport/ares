package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.events.AresKafkaEmitter
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
    lateinit var aresKafkaEmitter: AresKafkaEmitter

    @Inject
    lateinit var paymentMigration: PaymentMigration

    override suspend fun migratePaymentsFromSage(startDate: String?, endDate: String?, bpr: String, mode: String): Int {
        val paymentRecords = sageService.getPaymentDataFromSage(startDate, endDate, bpr, mode)
        logger().info("Total number of payment record to process : ${paymentRecords.size}")
        for (paymentRecord in paymentRecords) {
            aresKafkaEmitter.emitPaymentMigration(paymentRecord)
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
        for (jvRecord in jvRecords) {
            aresKafkaEmitter.emitJournalVoucherMigration(jvRecord)
        }
        return jvRecords.size
    }

    override suspend fun migratePaymentsByDate(bpr: String, mode: String): Int {
        val paymentRecords = sageService.migratePaymentsByDate(bpr, mode)
        logger().info("Total number of payment record to process : ${paymentRecords.size}")
        for (paymentRecord in paymentRecords) {
            aresKafkaEmitter.emitPaymentMigration(paymentRecord)
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
            aresKafkaEmitter.emitPaymentMigration(paymentRecord)
        }
        return paymentRecords.size
    }
}
