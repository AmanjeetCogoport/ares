package com.cogoport.ares.api.common.client

import com.cogoport.ares.model.dunning.request.TicketGenerationReq
import com.cogoport.ares.model.dunning.response.TicketGenerationResp
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Headers
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@Client(id = "cogo-care")
@Headers(
    Header(name = "microserviceAuthToken", value = "\${services.auth.microserviceAuthToken}"),
    Header(name = HttpHeaders.CONTENT_TYPE, value = "application/json")
)
interface CogoCareClient {
    @Post("/tickets/token")
    suspend fun getTicketToken(@Body request: TicketGenerationReq): TicketGenerationResp?
}
