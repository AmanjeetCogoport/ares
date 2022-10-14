package com.cogoport.ares.api.migration.controller

import com.cogoport.ares.api.migration.model.Data
import com.cogoport.ares.api.migration.service.interfaces.MigrationLogService
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.common.models.Response
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import jakarta.inject.Inject

@Controller
class MigratePaymentsController {

    @Inject lateinit var paymentMigration: PaymentMigrationWrapper
    @Inject lateinit var migrationLog: MigrationLogService

    @Get("/migrate")
    suspend fun migratePayments(@QueryValue startDate: String, @QueryValue endDate: String, @QueryValue migrationType: String): Response<String> {

        if (migrationType.equals("PAYMENTS", ignoreCase = true)) {
            val size = paymentMigration.migratePaymentsFromSage(startDate, endDate)
            return Response<String>().ok(
                HttpStatus.OK.name,
                "Request for payment migration received, total number of payments to migrate is $size"
            )
        } else {
            val size = paymentMigration.migrateJournalVoucher(startDate, endDate)
            return Response<String>().ok(
                HttpStatus.OK.name,
                "Request for journal voucher migration received, total number of jv to migrate is $size"
            )
        }
    }

    @Post("/push-to-aresmigration")
    suspend fun migrateToAres(@Body req: Data): String {
        return migrationLog.migrateToAres(req)
    }
}
