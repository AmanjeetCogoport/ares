package com.cogoport.ares.api.migration.scheduler

import com.cogoport.ares.api.events.AresMessagePublisher
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.api.migration.service.interfaces.SageService
import com.cogoport.ares.api.utils.logger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Singleton
class UtilizationUpdateScheduler {
    @Inject lateinit var paymentMigrationWrapper: PaymentMigrationWrapper

    @Inject lateinit var sageService: SageService

    @Inject lateinit var aresMessagePublisher: AresMessagePublisher

//    @Scheduled(cron = "0 30 5 ? * *")
    fun updateUtilizationValues() = runBlocking {
        try {
            logger().info("Running scheduler for updating payment utilization amount")
            val date: String? = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
            paymentMigrationWrapper.updateUtilizationAmount(null, null, date)
        } catch (ex: Exception) {
            logger().error("error while running payment scheduler  ${ex.message}")
        }
    }

//    @Scheduled(cron = "0 30 6 ? * *")
    fun updateUtilizationValuesForInvoices() = runBlocking {
        try {
            logger().info("Running scheduler for updating invoice utlization amount")
            val date: String? = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
            paymentMigrationWrapper.updateUtilizationForInvoice(null, null, date, null)
        } catch (ex: Exception) {
            logger().error("error while running invoice scheduler $ex")
        }
    }

//    @Scheduled(cron = "0 30 7 ? * *")
    fun updateUtilizationValuesForBills() = runBlocking {
        try {
            logger().info("Running scheduler for updating bill utilization amount")
            val date: String? = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
            paymentMigrationWrapper.updateUtilizationForBill(null, null, date)
        } catch (ex: Exception) {
            logger().error("error while running bill scheduler $ex")
        }
    }

//    @Scheduled(cron = "0 30 4 ? * *")
    fun migratePayments() = runBlocking {
        val endDate: String = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val startDate: String = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
        val records = sageService.getPaymentsForScheduler(startDate, endDate)
        records.forEach {
            aresMessagePublisher.emitPaymentMigration(it)
        }
        val jvRecords = sageService.getJVDetails(startDate, endDate, null, null)
        jvRecords.forEach {
            aresMessagePublisher.emitJournalVoucherMigration(it)
        }
    }

//    @Scheduled(cron = "0 30 8 ? * *")
    fun migrateJVUtilizationScheduler() = runBlocking {
        val endDate: String = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val startDate: String = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
        paymentMigrationWrapper.migrateJVUtilization(startDate, endDate, null)
    }
}
