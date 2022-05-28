package com.cogoport.ares.client

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.model.payment.OverallStats
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.MonthlyOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.SalesTrendResponse
import com.cogoport.ares.model.payment.ReceivableAgeingResponse
import com.cogoport.ares.model.payment.AccountCollectionResponse
import com.cogoport.ares.model.payment.Payment
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.AgeingBucket
import com.cogoport.ares.model.payment.CollectionTrend
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
    @Get("/dashboard/overall-stats")
    public suspend fun getOverallStats(
        @QueryValue(AresConstants.ROLE) zone: String?,
        @QueryValue(AresConstants.ZONE) role: String?
    ): OverallStats?

    @Get("/dashboard/daily-sales-outstanding-widget")
    public suspend fun getDailySalesOutstandingWidget(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?,
        @QueryValue(AresConstants.QUARTER) quarter: String
    ): DailySalesOutstanding?

    @Get("/dashboard/collection-trend")
    public suspend fun getCollectionTrend(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?,
        @QueryValue(AresConstants.QUARTER) quarter: String
    ): CollectionTrend?

    @Get("/dashboard/monthly-outstanding")
    public suspend fun getMonthlyOutstanding(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?
    ): MonthlyOutstanding?

    @Get("/dashboard/quarterly-outstanding")
    public suspend fun getQuarterlyOutstanding(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?
    ): QuarterlyOutstanding?

    @Get("/dashboard/sales-trend")
    public suspend fun getSalesTrend(
        @QueryValue(AresConstants.ZONE) zone: String?,
        @QueryValue(AresConstants.ROLE) role: String?
    ): SalesTrendResponse?

    @Get("/dashboard/outstanding-by-age")
    public suspend fun getOutStandingByAge(): List<AgeingBucket>?

    @Get("/dashboard/receivables-by-age")
    public suspend fun getReceivablesByAge(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?
    ): ReceivableAgeingResponse

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

    @Get("/outstanding/overall")
    suspend fun getOutstandingList(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?
    ): OutstandingList?
}
