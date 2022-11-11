package com.cogoport.ares.api.migration.controller

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

    @Get("/migrate")
    suspend fun migratePayments(
        @QueryValue startDate: String,
        @QueryValue endDate: String,
        @QueryValue migrationType: String,
        @QueryValue bpr: String,
        @QueryValue mode: String
    ): Response<String> {

        if (migrationType.equals("PAYMENTS", ignoreCase = true)) {
            val size = paymentMigration.migratePaymentsFromSage(startDate, endDate, bpr, mode)
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

    @Post("/migrate-payments-bpr")
    suspend fun migratePaymentsBpr(@QueryValue bpr: String, @QueryValue mode: String): Response<String> {
        val size = paymentMigration.migratePaymentsByBpr(bpr, mode)
        return Response<String>().ok(HttpStatus.OK.name, "Request for payment migration received, total number of payment to migrate is $size")
    }

    @Post("/paymentNum-migrate")
    suspend fun migratePaymentNum(@Body paymentNums: List<String>): Response<String> {
        val size = paymentMigration.migratePaymentsByPaymentNum(paymentNums)
        return Response<String>().ok(HttpStatus.OK.name, "Request for payment migration received, total number of payment to migrate is $size")
    }
}
