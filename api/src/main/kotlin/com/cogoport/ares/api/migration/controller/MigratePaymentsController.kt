package com.cogoport.ares.api.migration.controller

import com.cogoport.ares.api.migration.model.SettlementEntriesRequest
import com.cogoport.ares.api.migration.service.interfaces.PaymentMigrationWrapper
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.TdsAmountReq
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
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
            val size = paymentMigration.migrateJournalVoucherRecordNew(startDate, endDate, null, null)
            return Response<String>().ok(
                HttpStatus.OK.name,
                "Request for journal voucher migration received, total number of jv to migrate is $size"
            )
        }
    }

    @Post("/migrate-payments-date")
    suspend fun migratePaymentsBpr(@QueryValue startDate: String, @QueryValue endDate: String): Response<String> {
        val size = paymentMigration.migratePaymentsByDate(startDate, endDate)
        return Response<String>().ok(HttpStatus.OK.name, "Request for payment migration received, total number of payment to migrate is $size")
    }

    @Post("/paymentNum-migrate")
    suspend fun migratePaymentNum(@Body paymentNums: List<String>): Response<String> {
        val size = paymentMigration.migratePaymentsByPaymentNum(paymentNums)
        return Response<String>().ok(HttpStatus.OK.name, "Request for payment migration received, total number of payment to migrate is $size")
    }

    @Post("/JVNum-migrate")
    suspend fun migrateJVNum(@Body jvNums: List<String>): Response<String> {
        val size = paymentMigration.migrateJournalVoucherRecordNew(null, null, jvNums, null)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request for journal voucher migration received, total number of jv to migrate is $size"
        )
    }
    @Get("/migrate-settlements")
    suspend fun migrateSettlement(
        @QueryValue startDate: String,
        @QueryValue endDate: String
    ): Response<String> {
        val size = paymentMigration.migrateSettlementsWrapper(startDate, endDate, null)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request to migrate settlements received, total records: $size"
        )
    }

    @Post("/migrate-settlement-entries")
    suspend fun migrateSettlementEntries(@Body request: SettlementEntriesRequest): Response<String> {
        val size = paymentMigration.migrateSettlementsWrapper(request.startDate, request.endDate, request.entries)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request to migrate settlements received, total records: $size"
        )
    }
    @Get("/update-utilization-amount-date")
    suspend fun migrateUtilizationAmountByDate(
        @QueryValue startDate: String,
        @QueryValue endDate: String
    ): Response<String> {
        val count = paymentMigration.updateUtilizationAmount(startDate, endDate, null)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request received to update utilizations for payments total record: $count"
        )
    }

    @Post("/update-utilization-amount")
    suspend fun migrateUtilizationAmount(@Body request: List<String>): Response<String> {
        val count = paymentMigration.updateUtilizationAmountByPaymentNum(request)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request received to update utilizations for payments total record: $count"
        )
    }

    @Get("/update-invoice-utilization")
    suspend fun updateInvoiceUtilization(@QueryValue startDate: String, @QueryValue endDate: String): Response<String> {
        val count = paymentMigration.updateUtilizationForInvoice(startDate, endDate, null, null)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request received to update utilizations for invoice total record: $count"
        )
    }

    @Get("/update-bill-utilization")
    suspend fun updateBillUtilization(@QueryValue startDate: String, @QueryValue endDate: String): Response<String> {
        val count = paymentMigration.updateUtilizationForBill(startDate, endDate, null)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request received to update utilizations for bill total record: $count"
        )
    }

    @Post("/update-invoice-utilization-invoice-number")
    suspend fun updateInvoiceUtilizationByInvoiceNUmbers(@Body request: List<String>): Response<String> {
        val count = paymentMigration.updateUtilizationForInvoice(null, null, null, request)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request received to update utilizations for bill total record: $count"
        )
    }

    @Post("/settlementNum-migrate")
    suspend fun migrateSettlementNum(@Body settlementIds: List<Long>) {
        paymentMigration.migrateSettlementNumWrapper(settlementIds)
    }

    @Put("/tds-amount")
    suspend fun migrateTdsAmount(@Body req: List<TdsAmountReq>) {
        paymentMigration.migrateTdsAmount(req)
    }

    @Get("/migrate-jv")
    suspend fun migrateJvByDate(@QueryValue startDate: String, @QueryValue endDate: String): Response<String> {
        val count = paymentMigration.migrateJournalVoucherRecordNew(startDate, endDate, null, null)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request received to update utilizations for bill total record: $count"
        )
    }

    @Get("/new-pr")
    suspend fun migrateNewPrRecord(@QueryValue startDate: String, @QueryValue endDate: String, @QueryValue bpr: String?, @QueryValue accMode: String): String {
        paymentMigration.migrateNewPR(startDate, endDate, bpr, accMode)
        return Response<String>().ok(
            "request received to migrate new period"
        )
    }

    @Get("/migrate-jv-utilization")
    suspend fun migrateJVUtilization(@QueryValue startDate: String, @QueryValue endDate: String): String {
        paymentMigration.migrateJVUtilization(startDate, endDate, null)
        return Response<String>().ok(
            "request received to migrate new period"
        )
    }

    @Post("/migrate-jv-utilization-by-jv-num")
    suspend fun migrateJVUtilizationByJvNum(@Body jvNums: List<String>): String {
        paymentMigration.migrateJVUtilization(null, null, jvNums)
        return Response<String>().ok(
            "request received to migrate new period"
        )
    }

    @Get("/gl-account")
    suspend fun migratePaymentNum(): Response<String> {
        val size = paymentMigration.migrateGlAccount()
        return Response<String>().ok(HttpStatus.OK.name, "Request for GL code migration received, total number of GL to migrate is $size")
    }

    @Post("/migrate-sage-jv-id")
    suspend fun migrateJVNumById(@Body sageJvIds: List<String>): Response<String> {
        val size = paymentMigration.migrateJournalVoucherRecordNew(null, null, null, sageJvIds)
        return Response<String>().ok(
            HttpStatus.OK.name,
            "Request for journal voucher migration received, total number of jv to migrate is $size"
        )
    }
}
