package com.cogoport.ares.client

import com.cogoport.ares.model.payment.AccountPayablesFile
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.CustomerStatsRequest
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.DsoRequest
import com.cogoport.ares.model.payment.KamPaymentRequest
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.OrgPayableRequest
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.CollectionRequest
import com.cogoport.ares.model.payment.request.DeletePaymentRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.MonthlyOutstandingRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingAgeingRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import com.cogoport.ares.model.payment.request.OverallStatsRequest
import com.cogoport.ares.model.payment.request.QuarterlyOutstandingRequest
import com.cogoport.ares.model.payment.request.ReceivableRequest
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.AccountPayableFileResponse
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.BulkPaymentResponse
import com.cogoport.ares.model.payment.response.CollectionResponse
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse
import com.cogoport.ares.model.payment.response.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.response.OrgPayableResponse
import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsForCustomerResponse
import com.cogoport.ares.model.payment.response.OverallStatsResponse
import com.cogoport.ares.model.payment.response.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import io.micronaut.context.annotation.Parameter
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.multipart.StreamingFileUpload
import jakarta.validation.Valid

@Client(id = "ares")
interface AresClient {
    @Get("/payments/dashboard/overall-stats{?request*}")
    public suspend fun getOverallStats(@Valid request: OverallStatsRequest): OverallStatsResponse?

    @Get("/payments/dashboard/daily-sales-outstanding{?request*}")
    public suspend fun getDailySalesOutstanding(@Valid request: DsoRequest): DailySalesOutstanding?

    @Get("/payments/dashboard/collection-trend{?request*}")
    public suspend fun getCollectionTrend(@Valid request: CollectionRequest): CollectionResponse?

    @Get("/payments/dashboard/monthly-outstanding{?request*}")
    public suspend fun getMonthlyOutstanding(@Valid request: MonthlyOutstandingRequest): MonthlyOutstanding?

    @Get("/payments/dashboard/quarterly-outstanding{?request*}")
    public suspend fun getQuarterlyOutstanding(@Valid request: QuarterlyOutstandingRequest): QuarterlyOutstanding?

    @Get("/payments/dashboard/outstanding-by-age{?request*}")
    public suspend fun getOutStandingByAge(@Valid request: OutstandingAgeingRequest): List<OverallAgeingStatsResponse>?

    @Get("/payments/dashboard/receivables-by-age{?request*}")
    public suspend fun getReceivablesByAge(@Valid request: ReceivableRequest): ReceivableAgeingResponse

    @Get("/payments/dashboard/org-collection{?request*}")
    public suspend fun getOrgCollection(@Valid request: OrganizationReceivablesRequest): List<OutstandingResponse>

    @Get("/payments/dashboard/org-payables-stats{?request*}")
    public suspend fun getOrgPayables(@Valid request: OrgPayableRequest): OrgPayableResponse

    @Get("/payments/outstanding/overall{?request*}")
    suspend fun getOutstandingList(@Valid request: OutstandingListRequest): OutstandingList?

    @Get("/payments/outstanding/{orgId}")
    suspend fun getCustomerOutstanding(@PathVariable("orgId") orgId: String): MutableList<CustomerOutstanding?>

    @Get("/payments/accounts{?request*}")
    suspend fun getOnAccountCollections(@Valid request: AccountCollectionRequest): AccountCollectionResponse

    @Post("/payments/receivables/upload/{userId}", consumes = [MediaType.MULTIPART_FORM_DATA], produces = [MediaType.TEXT_PLAIN])
    suspend fun upload(@Parameter("file") file: StreamingFileUpload, @PathVariable("userId") userId: String): Boolean

    @Post("/payments/accounts")
    suspend fun createOnAccountReceivables(@Valid @Body request: Payment): OnAccountApiCommonResponse

    @Put("/payments/accounts")
    suspend fun updateOnAccountReceivables(@Valid @Body request: Payment): OnAccountApiCommonResponse

    @Delete("/payments/accounts")
    suspend fun deleteOnAccountReceivables(@Body deletePaymentRequest: DeletePaymentRequest): OnAccountApiCommonResponse

    @Post("/payments/accounts/bulk-create")
    suspend fun createBulkOnAccountPayment(@Valid @Body request: MutableList<Payment>): BulkPaymentResponse

    @Post("/payments/invoice/add-bulk")
    suspend fun createBulkInvoice(@Valid @Body invoiceRequestList: List<AccUtilizationRequest>): MutableList<CreateInvoiceResponse>

    @Post("/payments/invoice")
    suspend fun createInvoice(@Valid @Body invoiceRequest: AccUtilizationRequest): CreateInvoiceResponse

    @Delete("/payments/invoice")
    suspend fun deleteInvoice(@QueryValue("docNumber") docNumber: Long, @QueryValue("accType") accType: String): Boolean

    @Post("/payments/knockoff/payables")
    suspend fun knockOffPayables(@Valid @Body payableList: List<AccountPayablesFile>): MutableList<AccountPayableFileResponse>

    @Get("/payments/accounts/ledger-summary{?request*}")
    suspend fun getOrganizationAccountUtilization(@Valid request: LedgerSummaryRequest): List<AccountUtilizationResponse?>

    @Post("/payments/dashboard/overallStatsForKam")
    suspend fun getOverallStatsForKam(@Valid @Body request: KamPaymentRequest): StatsForKamResponse

    @Post("/payments/dashboard/overallStatsForCustomers")
    suspend fun getOverallStatsForCustomers(
        @Valid @Body request: CustomerStatsRequest
    ): List<OverallStatsForCustomerResponse>
}
