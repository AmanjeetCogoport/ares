package com.cogoport.ares.api.common.config

import com.cogoport.brahma.authentication.Authentication
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.UUID

@Filter("/**")
class RequestIdMdcFilter : HttpServerFilter {

    @Value("\${environment}")
    private lateinit var environment: String

    companion object {
        const val TRACE_ID_MDC_KEY = "traceId"
        val LOG: Logger = LoggerFactory.getLogger(RequestIdMdcFilter::class.java)
    }

    override fun doFilter(request: HttpRequest<*>?, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        var traceIdHeader = request?.headers?.get(TRACE_ID_MDC_KEY)

        if (traceIdHeader == null) {
            traceIdHeader = UUID.randomUUID().toString()
        }

        if (MDC.get(TRACE_ID_MDC_KEY) != null) {
            LOG.warn("MDC should have been empty here.")
        }

        LOG.debug("Storing traceId in MDC: $traceIdHeader")

        MDC.put(TRACE_ID_MDC_KEY, traceIdHeader)

        return Authentication.authenticate(request, chain, environment)
    }
}
