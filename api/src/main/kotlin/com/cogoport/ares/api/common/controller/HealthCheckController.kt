package com.cogoport.ares.api.common.controller
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/health")
class HealthCheckController {

    @Get
    fun healthCheck(): HttpResponse<Map<String, String>> {
        return HttpResponse.ok(mapOf("status" to "ok"))
    }
}
