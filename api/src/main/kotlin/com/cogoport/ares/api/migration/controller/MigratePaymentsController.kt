package com.cogoport.ares.api.migration.controller

import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.common.models.Response
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import jakarta.inject.Inject

@Controller
class MigratePaymentsController {

    @Inject lateinit var paymentMigration: PaymentMigrationWrapper

    @Get("/migrate")
    suspend fun migratePayments(@QueryValue startDate: String, @QueryValue endDate: String): Response<String> {
        val size = paymentMigration.migratePaymentsFromSage(startDate, endDate)
        return Response<String>().ok(HttpStatus.OK.name, "Request for payment migration received, total number of payments to migrate is $size")
    }
}
