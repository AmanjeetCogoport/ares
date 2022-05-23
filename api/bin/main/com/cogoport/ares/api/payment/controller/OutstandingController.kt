package com.cogoport.ares.api.payment.controller

import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.api.payment.service.interfaces.OutStandingService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject

@Validated
@Controller("/outstanding")
class OutstandingController {
    @Inject
    lateinit var outStandingService: OutStandingService

    @Get("/overall")
    suspend fun getOutstandingList(
        @QueryValue("zone") zone: String?,
        @QueryValue("role") role: String?
    ): OutstandingList? {
        return Response<OutstandingList?>().ok(outStandingService.getOutstandingList(zone, role))
    }
}
