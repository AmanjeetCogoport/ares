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
                tradePartyDetailId = UUID.fromString("1aca1260-9813-4fa1-9af3-fb1db2a63b22")
            )
        )

        return HttpResponse.ok(mapOf("status" to "ok"))
    }
}
