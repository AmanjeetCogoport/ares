package com.cogoport.ares.client

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.payment.entity.CollectionTrend
import com.cogoport.ares.api.payment.entity.OverallStats
import com.cogoport.ares.api.payment.model.CollectionRequest
import com.cogoport.ares.api.payment.model.DsoRequest
import com.cogoport.ares.api.payment.model.MonthlyOutstandingRequest
import com.cogoport.ares.api.payment.model.OutstandingAgeingRequest
import com.cogoport.ares.api.payment.model.OutstandingListRequest
import com.cogoport.ares.api.payment.model.OverallStatsRequest
import com.cogoport.ares.api.payment.model.QuarterlyOutstandingRequest
import com.cogoport.ares.api.payment.model.ReceivableRequest
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.SalesTrendResponse
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.AgeingBucket
import io.micronaut.context.annotation.Parameter
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.multipart.StreamingFileUpload
import jakarta.validation.Valid
import java.time.LocalDateTime

@Client("http://localhost:8087/payment")
interface AresClient {
    @Get("/dashboard/overall-stats{?request*}")
    public suspend fun getOverallStats(@Valid request: OverallStatsRequest): OverallStats?

    @Get("/dashboard/daily-sales-outstanding{?request*}")
    public suspend fun getDailySalesOutstanding(@Valid request: DsoRequest): DailySalesOutstanding?

    @Get("/dashboard/collection-trend{?request*}")
    public suspend fun getCollectionTrend(@Valid request: CollectionRequest): CollectionTrend?

    @Get("/dashboard/monthly-outstanding{?request*}")
    public suspend fun getMonthlyOutstanding(@Valid request: MonthlyOutstandingRequest): MonthlyOutstanding?

    @Get("/dashboard/quarterly-outstanding{?request*}")
    public suspend fun getQuarterlyOutstanding(@Valid request: QuarterlyOutstandingRequest): QuarterlyOutstanding?

    /** Sales trend need to be deleted */
    @Get("/dashboard/sales-trend")
    public suspend fun getSalesTrend(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?
    ): SalesTrendResponse?

    @Get("/dashboard/outstanding-by-age{?request*}")
    public suspend fun getOutStandingByAge(@Valid request: OutstandingAgeingRequest): List<AgeingBucket>?

    @Get("/dashboard/receivables-by-age{?request*}")
    public suspend fun getReceivablesByAge(@Valid request: ReceivableRequest): ReceivableAgeingResponse

    @Get("/receivables/collections")
    suspend fun getOnAccountCollections(
        @QueryValue("uploadedDate") uploadedDate: LocalDateTime?,
        @QueryValue("entityType") entityType: Int?,
        @QueryValue("currencyType") currencyType: String?
    ): AccountCollectionResponse
    @Post("/receivables/upload/{userId}", consumes = [MediaType.MULTIPART_FORM_DATA], produces = [MediaType.TEXT_PLAIN])
    suspend fun upload(@Parameter("file") file: StreamingFileUpload, @PathVariable("userId") userId: String): Boolean

    @Post("/receivables/collection/create")
    suspend fun createOnAccountReceivables(@Valid @Body request: Payment): Payment

    @Get("/outstanding/overall{?request*}")
    suspend fun getOutstandingList(@Valid request: OutstandingListRequest): OutstandingList?
}
