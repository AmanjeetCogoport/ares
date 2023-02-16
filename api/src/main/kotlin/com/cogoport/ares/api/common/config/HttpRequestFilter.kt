package com.cogoport.ares.api.common.config

import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.ClientFilterChain
import io.micronaut.http.filter.HttpClientFilter
import org.reactivestreams.Publisher

@Filter(serviceId = ["loki", "plutus", "ares", "heimdall", "hades", "demeter"])
class HttpRequestFilter : HttpClientFilter {

    @Value("\${services.auth.microserviceAuthToken}")
    private lateinit var microserviceAuthToken: String

    override fun doFilter(request: MutableHttpRequest<*>?, chain: ClientFilterChain?): Publisher<out HttpResponse<*>>? {
        request?.header("microserviceAuthToken", microserviceAuthToken)
        return chain?.proceed(request)
    }
}
