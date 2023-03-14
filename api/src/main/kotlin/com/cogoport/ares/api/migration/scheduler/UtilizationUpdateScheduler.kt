package com.cogoport.ares.api.migration.scheduler

import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.api.utils.logger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Singleton
class UtilizationUpdateScheduler {
    @Inject lateinit var paymentMigrationWrapper: PaymentMigrationWrapper

    /**
     * turning off scheduler as for testing
     * **/
//    @Scheduled(cron = "0 30 5 ? * *")
    fun updateUtilizationValues() = runBlocking {
        try {
            logger().info("Running scheduler for updating payment utilization amount")
            val date: String? = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)
            paymentMigrationWrapper.updateUtilizationAmount(null, null, date)
        } catch (ex: Exception) {
            logger().error("error while running payment scheduler  $ex")
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
}
