package com.cogoport.ares.client

import com.cogoport.ares.model.payment.AccUtilizationRequest
import com.cogoport.ares.model.payment.AccountCollectionRequest
import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.AccountPayableFileResponse
import com.cogoport.ares.model.payment.AccountPayablesFile
import com.cogoport.ares.model.payment.BulkPaymentResponse
import com.cogoport.ares.model.payment.CollectionRequest
import com.cogoport.ares.model.payment.CollectionResponse
import com.cogoport.ares.model.payment.CreateInvoiceResponse
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.OutstandingListRequest
import com.cogoport.ares.model.payment.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.OverallStatsRequest
import com.cogoport.ares.model.payment.OverallStatsResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.ReceivableRequest
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

@Client(id = "ares-service")
interface AresClient {
    @Get("/payment/dashboard/overall-stats{?request*}")
    public suspend fun getOverallStats(@Valid request: OverallStatsRequest): OverallStatsResponse?

    @Get("/payment/dashboard/daily-sales-outstanding{?request*}")
    public suspend fun getDailySalesOutstanding(@Valid request: DsoRequest): DailySalesOutstanding?

    @Get("/payment/dashboard/collection-trend{?request*}")
    public suspend fun getCollectionTrend(@Valid request: CollectionRequest): CollectionResponse?

    @Get("/payment/dashboard/monthly-outstanding{?request*}")
    public suspend fun getMonthlyOutstanding(@Valid request: MonthlyOutstandingRequest): MonthlyOutstanding?

    @Get("/payment/dashboard/quarterly-outstanding{?request*}")
    public suspend fun getQuarterlyOutstanding(@Valid request: QuarterlyOutstandingRequest): QuarterlyOutstanding?

    @Get("/payment/dashboard/outstanding-by-age{?request*}")
    public suspend fun getOutStandingByAge(@Valid request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse>?

    @Get("/payment/dashboard/receivables-by-age{?request*}")
    public suspend fun getReceivablesByAge(@Valid request: ReceivableRequest): ReceivableAgeingResponse

    @Get("/payment/outstanding/overall{?request*}")
    suspend fun getOutstandingList(@Valid request: OutstandingListRequest): OutstandingList?

    @Get("/payment/outstanding/{orgId}")
    suspend fun getCustomerOutstanding(@PathVariable("orgId") orgId: String): MutableList<CustomerOutstanding?>

    @Get("/payment/accounts{?request*}")
    suspend fun getOnAccountCollections(@Valid request: AccountCollectionRequest): AccountCollectionResponse

    @Post("/payment/receivables/upload/{userId}", consumes = [MediaType.MULTIPART_FORM_DATA], produces = [MediaType.TEXT_PLAIN])
    suspend fun upload(@Parameter("file") file: StreamingFileUpload, @PathVariable("userId") userId: String): Boolean

    @Post("/payment/accounts")
    suspend fun createOnAccountReceivables(@Valid @Body request: Payment): OnAccountApiCommonResponse

    @Put("/payment/accounts")
    suspend fun updateOnAccountReceivables(@Valid @Body request: Payment): OnAccountApiCommonResponse

    @Delete("/payment/accounts")
    suspend fun deleteOnAccountReceivables(@QueryValue("paymentId") paymentId: Long): OnAccountApiCommonResponse

    @Post("/payment/accounts/bulk-create")
    suspend fun createBulkOnAccountPayment(@Valid @Body request: MutableList<Payment>): BulkPaymentResponse

    @Post("/payment/invoice/add-bulk")
    suspend fun createBulkInvoice(@Valid @Body invoiceRequestList: List<AccUtilizationRequest>): MutableList<CreateInvoiceResponse>

    @Post("/payment/invoice")
    suspend fun createInvoice(@Valid @Body invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse

    @Delete("/payment/invoice")
    suspend fun deleteInvoice(@QueryValue("docNumber") docNumber: Long, @QueryValue("accType") accType: String): Boolean

    @Post("/payment/knockoff/payables")
    suspend fun knockOffPayables(@Valid @Body payableList: List<AccountPayablesFile>): MutableList<AccountPayableFileResponse>
}
