package com.cogoport.ares.api.common.controller
import com.cogoport.ares.api.dunning.model.request.PaymentReminderReq
import com.cogoport.ares.api.dunning.service.interfaces.ScheduleService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import java.util.UUID

@Controller("/health")
class HealthCheckController(
    private val railsClient: ScheduleService,
) {

    @Get
    suspend fun healthCheck(): HttpResponse<Map<String, String>> {
        val a = railsClient.sendPaymentReminderToTradeParty(
            PaymentReminderReq(
                cycleExecutionId = 1,
                tradePartyDetailId = UUID.fromString("13637b93-fe15-44d5-a979-3c7dc3c2403f")
            )
        )

        return HttpResponse.ok(mapOf("status" to "ok"))
    }
}
