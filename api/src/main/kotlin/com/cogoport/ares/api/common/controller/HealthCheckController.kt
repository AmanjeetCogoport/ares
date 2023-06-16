package com.cogoport.ares.api.common.controller
import com.cogoport.ares.api.common.client.RailsClient
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import java.util.UUID

@Controller("/health")
class HealthCheckController(
    private val railsClient: RailsClient
) {

    @Get
    suspend fun healthCheck(): HttpResponse<Map<String, String>> {

        val a = railsClient.getCpUsers(UUID.fromString("6cdd0732-e9bd-4c3a-8cea-c4b30b734ba2"))

        println("cp user$a")

        val b = railsClient.listOrgUsers(UUID.fromString("504f1c67-d3c2-49c5-823c-3c3d1b616626")).list
        println("cp user$b")

        val c = railsClient.listTradeParties(UUID.fromString("d207ac61-bcfc-4572-8a9f-ea1340eb6c23"))
        println("cp user$c")

        return HttpResponse.ok(mapOf("status" to "ok"))
    }
}
