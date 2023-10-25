package com.cogoport.ares.client

import com.cogoport.ares.model.common.DeleteConsolidatedInvoicesReq
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.common.TdsAmountReq
import com.cogoport.ares.model.payment.AccountPayablesFile
import com.cogoport.ares.model.payment.CustomerOutstanding
import com.cogoport.ares.model.payment.CustomerStatsRequest
import com.cogoport.ares.model.payment.KamPaymentRequest
import com.cogoport.ares.model.payment.OrgPayableRequest
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.UpdateCSDPaymentRequest
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.request.AccountCollectionRequest
import com.cogoport.ares.model.payment.request.CustomerMonthlyPaymentRequest
import com.cogoport.ares.model.payment.request.DeletePaymentRequest
import com.cogoport.ares.model.payment.request.ExchangeRateForPeriodRequest
import com.cogoport.ares.model.payment.request.InvoiceListRequestForTradeParty
import com.cogoport.ares.model.payment.request.InvoicePaymentRequest
import com.cogoport.ares.model.payment.request.LedgerSummaryRequest
import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest
import com.cogoport.ares.model.payment.request.OnAccountTotalAmountRequest
import com.cogoport.ares.model.payment.request.OrganizationReceivablesRequest
import com.cogoport.ares.model.payment.request.OutstandingListRequest
import com.cogoport.ares.model.payment.request.SaasInvoiceHookRequest
import com.cogoport.ares.model.payment.request.TradePartyStatsRequest
import com.cogoport.ares.model.payment.request.UpdateOrganizationDetailAresSideRequest
import com.cogoport.ares.model.payment.response.AccountCollectionResponse
import com.cogoport.ares.model.payment.response.AccountPayableFileResponse
import com.cogoport.ares.model.payment.response.AccountUtilizationResponse
import com.cogoport.ares.model.payment.response.BulkPaymentResponse
import com.cogoport.ares.model.payment.response.CreateInvoiceResponse
import com.cogoport.ares.model.payment.response.CustomerMonthlyPayment
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.InvoicePaymentResponse
import com.cogoport.ares.model.payment.response.OnAccountApiCommonResponse
import com.cogoport.ares.model.payment.response.OnAccountTotalAmountResponse
import com.cogoport.ares.model.payment.response.OrgPayableResponse
import com.cogoport.ares.model.payment.response.OutstandingResponse
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
import com.cogoport.ares.model.payment.response.SaasInvoiceHookResponse
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import com.cogoport.ares.model.settlement.request.ParentJVUpdateRequest
import io.micronaut.context.annotation.Parameter
import io.micronaut.http.HttpResponse
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
import java.math.BigDecimal
import java.util.UUID
import kotlin.collections.HashMap

@Client(id = "ares")
interface AresClient {

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

    @Post("/payments/accounts/update-vendor-trade-party-data")
    suspend fun updateVendorTradePartyData(
        @Valid @Body
        request: UpdateOrganizationDetailAresSideRequest
    ): MutableMap<String, String>?

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

    @Post("/payments/dashboard/kam/overall-stats")
    suspend fun getOverallStatsForKam(@Valid @Body request: KamPaymentRequest): StatsForKamResponse

    @Post("/payments/dashboard/customer/overall-stats")
    suspend fun getOverallStatsForCustomers(
        @Valid @Body request: CustomerStatsRequest
    ): ResponseList<StatsForCustomerResponse?>

    @Delete("/payments/accounts/consolidated")
    suspend fun deleteConsolidatedInvoices(@Body req: DeleteConsolidatedInvoicesReq)

    @Get("/payments/service-discovery/reachability")
    suspend fun reachable(): HttpResponse<String>

    @Get("/payments/accounts/on-account-payment{?request*}")
    suspend fun onAccountPaymentValue(@Valid request: OnAccountTotalAmountRequest): MutableList<OnAccountTotalAmountResponse>
    @Post("/payments/outstanding/outstanding-days")
    suspend fun getCurrOutstanding(@Body invoiceIds: List<Long>): Long

    @Get("/payments/invoice/payment-status{?invoicePaymentRequest*}")
    suspend fun getInvoicePaymentStatus(@Valid invoicePaymentRequest: InvoicePaymentRequest): InvoicePaymentResponse?

    @Get("/payments/defaulters/list/trade-party-detail-ids")
    suspend fun listTradePartyDetailIds(): List<UUID>?

    @Get("/payments/dashboard/exchange-rate/for/period{?request*}")
    suspend fun getExchangeRateForPeriod(@Valid request: ExchangeRateForPeriodRequest): HashMap<String, BigDecimal>

    @Post("/payments/outstanding/customer-outstanding")
    suspend fun getCustomersOutstandingInINR(@Body orgIds: List<String>): MutableMap<String, BigDecimal?>

    @Post("/payments/dashboard/trade-party/stats")
    suspend fun getOverallStatsForTradeParties(
        @Valid @Body request: TradePartyStatsRequest
    ): ResponseList<OverallStatsForTradeParty?>

    @Post("/payments/dashboard/trade-party/invoice/list")
    suspend fun getInvoiceListForTradeParties(
        @Valid @Body request: InvoiceListRequestForTradeParty
    ): ResponseList<InvoiceListResponse?>

    @Get("/payments/invoice/amount-mismatch")
    suspend fun getInvoicesAmountMismatch(): List<Long>?

    @Get("/payments/invoice/missing-invoices")
    suspend fun getInvoicesNotPresentInAres(): List<Long>?

    @Post("/payments/settlement/settle-tagged-invoice-payment")
    suspend fun settleOnAccountTaggedInvoicePayment(@Body req: OnAccountPaymentRequest)

    @Put("/payments/tds-amount")
    suspend fun migrateTdsAmount(@Body req: List<TdsAmountReq>)

    @Get("/payments/cron-jobs/sales-amount-mismatch")
    suspend fun getSalesAmountMismatchInJobs(): List<Long>?

    @Get("/payments/cron-jobs/purchase-amount-mismatch")
    suspend fun getPurchaseAmountMismatchInJobs(): List<Long>?

    @Post("/payments/parent-jv/update")
    suspend fun updateParentJv(@Body req: ParentJVUpdateRequest): String

    @Get("/payments/outstanding/customer-monthly-payment{?request*}")
    suspend fun getCustomerMonthlyPayment(@Valid request: CustomerMonthlyPaymentRequest): CustomerMonthlyPayment

    @Post("/payments/accounts/update-csd-payment")
    suspend fun updateCSDPayments(@Valid @Body request: UpdateCSDPaymentRequest)

    @Post("/payments/accounts/saas-invoice-hook")
    suspend fun saasInvoiceHook(@Valid @Body req: SaasInvoiceHookRequest): SaasInvoiceHookResponse
}
