package com.cogoport.ares.api.exception

import com.cogoport.ares.api.utils.logger
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.hateoas.JsonError
import io.sentry.Sentry
import org.slf4j.MDC
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException

@Controller
class ExceptionHandler() {
    private val logger = logger()

    @Error(global = true)
    fun handle(exception: Exception?): HttpResponse<ErrorResponse> {

        exception?.let {
            logger.error(exception.message)
            Sentry.withScope {
                it.setTag("traceId", MDC.get("traceId"))
                it.setTag("spanId", MDC.get("spanId"))
                Sentry.captureException(exception)
            }
        }

        return when (exception) {
            is HttpStatusException -> HttpResponse.ok(exception.message).body(
                ErrorResponse(
                    exception.status.code,
                    exception.message
                )
            )
            is ConstraintViolationException, is ValidationException -> {
                HttpResponse.ok(exception.message).body(
                    ErrorResponse(
                        HttpStatus.BAD_REQUEST.code,
                        exception.message
                    )
                )
            }
            else -> HttpResponse.ok(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.code,
                    exception?.message
                )
            )
        }
    }

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    fun notFound(request: HttpRequest<*>?): HttpResponse<*> {
        return HttpResponse.notFound<JsonError>()
            .body(
                ErrorResponse(
                    HttpStatus.NOT_FOUND.code,
                    "Resource not found"
                )
            )
    }
}
