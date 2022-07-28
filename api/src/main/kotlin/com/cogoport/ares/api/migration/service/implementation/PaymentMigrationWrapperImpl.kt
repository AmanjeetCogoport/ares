package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.migration.service.interfaces.PaymentMigration
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.api.migration.service.interfaces.SageService
import com.cogoport.ares.api.utils.logger
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class PaymentMigrationWrapperImpl : PaymentMigrationWrapper {

    @Inject lateinit var sageService: SageService

    @Inject lateinit var paymentMigration: PaymentMigration

    override suspend fun migratePaymentsFromSage(startDate: String?, endDate: String?): Int {
        val paymentRecords = sageService.getPaymentDataFromSage(startDate, endDate)
        logger().info("Total number of payment record to process : ${paymentRecords.size}")
        var count = 0
        for (paymentRecord in paymentRecords) {
            paymentMigration.migratePayment(paymentRecord)
            logger().info("Migrated ${count++} / ${paymentRecords.size} payments")
        }
        return paymentRecords.size
    }
}
