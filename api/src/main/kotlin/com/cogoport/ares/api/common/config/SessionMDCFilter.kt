package com.cogoport.ares.api.common.config

import com.cogoport.ares.api.common.config.SessionMDCFilter.Keys.SESSION_INFO_KEY
import com.cogoport.ares.api.utils.logger
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import org.slf4j.MDC

@Filter("/**")
class SessionMDCFilter : HttpServerFilter {
    val logger = logger()
    override fun doFilter(request: HttpRequest<*>?, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        try {
            MDC.put(
                SESSION_INFO_KEY, """"""
            )
            logRequest(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return chain.proceed(request)
    }

    private fun logRequest(request: HttpRequest<*>?) {
        val stringBuilder = StringBuilder()
        stringBuilder.append("{")
        stringBuilder.append("'").append("method").append("':")
        stringBuilder.append("'").append(request?.method).append("'")
        stringBuilder.append(",'").append("path").append("':")
        stringBuilder.append("'").append(request?.path).append("'")
        stringBuilder.append(",'").append("remoteHost").append("':")
        stringBuilder.append("'").append(request?.remoteAddress?.address).append("'")

        request?.headers?.forEach {
            stringBuilder.append(",'").append(it.key).append("':")
            stringBuilder.append("'").append(it.value.toString().replace("\"", "")).append("'")
        }

        stringBuilder.append("}")

        logger.info(stringBuilder.toString())
    }

    object Keys {
        const val SESSION_INFO_KEY = "sessionInfo"
    }
}
