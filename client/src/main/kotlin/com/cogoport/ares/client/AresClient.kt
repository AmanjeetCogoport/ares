package com.cogoport.ares.client

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.BulkPaymentResponse
import com.cogoport.ares.model.payment.CollectionRequest
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.OutstandingListRequest
import com.cogoport.ares.model.payment.OverallStatsRequest
import com.cogoport.ares.model.payment.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.ReceivableRequest
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.SalesTrendResponse
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.OverallStatsResponse
import com.cogoport.ares.model.payment.CustomerOutstanding
import io.micronaut.context.annotation.Parameter
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.multipart.StreamingFileUpload
import jakarta.validation.Valid
import java.time.LocalDateTime
import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import com.cogoport.ares.model.payment.AccountPayableFileResponse
import com.cogoport.ares.model.payment.AccountPayablesFile

@Client(id = "ares-service")
interface AresClient {
    @Get("/dashboard/overall-stats{?request*}")
    public suspend fun getOverallStats(@Valid request: OverallStatsRequest): OverallStatsResponse?

    @Get("/dashboard/daily-sales-outstanding{?request*}")
    public suspend fun getDailySalesOutstanding(@Valid request: DsoRequest): DailySalesOutstanding?

    @Get("/dashboard/collection-trend{?request*}")
    public suspend fun getCollectionTrend(@Valid request: CollectionRequest): CollectionResponse?

    @Get("/dashboard/monthly-outstanding{?request*}")
    public suspend fun getMonthlyOutstanding(@Valid request: MonthlyOutstandingRequest): MonthlyOutstanding?

    @Get("/dashboard/quarterly-outstanding{?request*}")
    public suspend fun getQuarterlyOutstanding(@Valid request: QuarterlyOutstandingRequest): QuarterlyOutstanding?

    /** Sales trend need to be deleted */
    @Get("/dashboard/sales-trend")
    public suspend fun getSalesTrend(
        @QueryValue(AresModelConstants.ZONE) zone: String?,
        @QueryValue(AresModelConstants.ROLE) role: String?
    ): SalesTrendResponse?

    @Get("/dashboard/outstanding-by-age{?request*}")
    public suspend fun getOutStandingByAge(@Valid request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse>?

    @Get("/dashboard/receivables-by-age{?request*}")
    public suspend fun getReceivablesByAge(@Valid request: ReceivableRequest): ReceivableAgeingResponse

    @Get("/outstanding/overall{?request*}")
    suspend fun getOutstandingList(@Valid request: OutstandingListRequest): OutstandingList?

    @Get("/outstanding/{orgId}")
    suspend fun getCustomerOutstanding(@PathVariable("orgId") orgId: String): MutableList<CustomerOutstanding?>

    @Get("/accounts")
    suspend fun getOnAccountCollections(
        @QueryValue("uploadedDate") uploadedDate: LocalDateTime?,
        @QueryValue("entityType") entityType: Int?,
        @QueryValue("currencyType") currencyType: String?
    ): AccountCollectionResponse
    @Post("/receivables/upload/{userId}", consumes = [MediaType.MULTIPART_FORM_DATA], produces = [MediaType.TEXT_PLAIN])
    suspend fun upload(@Parameter("file") file: StreamingFileUpload, @PathVariable("userId") userId: String): Boolean

    @Post("/accounts")
    suspend fun createOnAccountReceivables(@Valid @Body request: Payment): Payment

    @Put("/accounts")
    suspend fun updateOnAccountReceivables(@Valid @Body request: Payment): Payment

    @Delete("/accounts")
    suspend fun deleteOnAccountReceivables(@QueryValue("paymentId") paymentId: Long): String

    @Post("/accounts/bulk-create")
    suspend fun createBulkOnAccountPayment(@Valid @Body request: MutableList<Payment>): BulkPaymentResponse

    @Post("/invoice/add-bulk")
    suspend fun createBulkInvoice(@Valid @Body invoiceRequestList: List<AccUtilizationRequest>): MutableList<CreateInvoiceResponse>

    @Post("/invoice")
    suspend fun createInvoice(@Valid @Body invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse

    @Delete("/invoice")
    suspend fun deleteInvoice(@QueryValue("docNumber") docNumber: Long, @QueryValue("accType") accType: String): Boolean

    @Post("/knockoff/payables")
    suspend fun knockOffPayables(@Valid @Body payableList: List<AccountPayablesFile>): MutableList<AccountPayableFileResponse>
}
