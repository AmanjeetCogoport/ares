package com.cogoport.ares.api.common.controller
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/service-health-check")
class HealthCheckController {

    @Get("/healthcheck")
    fun healthCheck(): HttpResponse<Map<String, String>> {
        try {
            return HttpResponse
                .ok(mapOf("status" to "ok"))
        } catch (err: Exception) {
            throw AresException(AresError.ERR_1507, "")
        }
    }
}
