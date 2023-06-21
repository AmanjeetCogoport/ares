package com.cogoport.ares.api.common.controller
import com.cogoport.ares.api.common.client.RailsClient
import com.cogoport.ares.api.dunning.model.request.CycleExecutionProcessReq
import com.cogoport.ares.api.dunning.service.interfaces.ScheduleService
import com.cogoport.brahma.hashids.Hashids
import com.cogoport.brahma.rabbitmq.client.interfaces.RabbitmqService
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import java.text.SimpleDateFormat
import java.util.*

@Controller("/health")
class HealthCheckController(
    private val railsClient: RailsClient,
    private val sService: ScheduleService,
    private val rabbitMq: RabbitmqService
) {

    @Get
    suspend fun healthCheck(): HttpResponse<Map<String, String>> {

        val calendar = Calendar.getInstance()
        val currentDate = calendar.time
        calendar.add(Calendar.MINUTE, 5)
        val fiveMinutesLater = calendar.time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val formattedDate = dateFormat.format(fiveMinutesLater)
        val date: Date = dateFormat.parse(formattedDate)

        val request = ObjectMapper().writeValueAsString(
            CycleExecutionProcessReq(
                scheduleId = Hashids.encode(2)
            )
        )

        rabbitMq.delay("ares.dunning.scheduler", request, date)

//        val d = sService.processCycleExecution(CycleExecutionProcessReq("yo"))

//        val a = railsClient.getCpUsers(UUID.fromString("6cdd0732-e9bd-4c3a-8cea-c4b30b734ba2"))
//
//        println("cp user$a")
//
//        val b = railsClient.listOrgUsers(UUID.fromString("504f1c67-d3c2-49c5-823c-3c3d1b616626")).list
//        println("cp user$b")
//
//        val c = railsClient.listTradeParties(UUID.fromString("d207ac61-bcfc-4572-8a9f-ea1340eb6c23"))
//        println("cp user$c")

        return HttpResponse.ok(mapOf("status" to "ok"))
    }
}
