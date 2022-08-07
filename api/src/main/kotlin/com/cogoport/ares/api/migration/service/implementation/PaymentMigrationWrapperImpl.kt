package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.events.AresKafkaEmitter
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

    override suspend fun migratePaymentsFromSage(startDate: String?, endDate: String?): Int {
        val paymentRecords = sageService.getPaymentDataFromSage(startDate, endDate)
        logger().info("Total number of payment record to process : ${paymentRecords.size}")
        for (paymentRecord in paymentRecords) {
            aresKafkaEmitter.emitPaymentMigration(paymentRecord)
        }
        return paymentRecords.size
    }
}
