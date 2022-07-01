package com.cogoport.ares.api.exception

import com.cogoport.ares.api.common.models.ErrorResponse
import com.cogoport.ares.api.utils.logger
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Error
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.sentry.Sentry
import jakarta.inject.Singleton
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException
import org.slf4j.MDC

@Singleton
class AresExceptionHandler : ExceptionHandler<Exception, HttpResponse<ErrorResponse>> {
    private val logger = logger()

    @Error(global = true, exception = Exception::class)
    override fun handle(request: HttpRequest<*>?, exception: Exception?): HttpResponse<ErrorResponse> {

        sendToSentry(exception)
        logger.error(request.toString(), exception)
        var errorMessage: ErrorResponse

        when (exception) {
            is AresException -> {
                errorMessage = exception?.error?.let {
                    ErrorResponse(
                        it.code,
                        it.getMessage(exception.context),
                        it.httpStatus,
                        exception
                    )
                }!!
            }
            is HttpStatusException -> {
                errorMessage = ErrorResponse(
                    AresError.ERR_1000.code,
                    exception.message,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    exception
                )
            }
            is ConstraintViolationException, is ValidationException -> {
                errorMessage = ErrorResponse(
                    AresError.ERR_1001.code,
                    exception.message,
                    HttpStatus.BAD_REQUEST,
                    exception
                )
            }
            else -> {
                errorMessage = ErrorResponse(
                    AresError.ERR_1001.code,
                    exception?.message,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception
                )
            }
        }
        return getResponse(errorMessage?.httpStatus, errorMessage)
    }

    private fun sendToSentry(exception: Exception?) {
        exception?.let {
            logger.error(exception.message)
            Sentry.withScope {
                it.setTag("traceId", MDC.get("traceId"))
                it.setTag("spanId", MDC.get("spanId"))
                Sentry.captureException(exception)
            }
        }
    }

    /**
     * Generate HTTPErrorResponse based on HttpStatus and ErrorResponse
     * @param HttpStatus, ErrorResponse
     * @return HttpResponse<ErrorResponse>
     */
    private fun getResponse(httpStatus: HttpStatus?, errorMessage: ErrorResponse): HttpResponse<ErrorResponse> {
        return if (httpStatus?.equals(HttpStatus.BAD_REQUEST) == true)
            HttpResponse.badRequest(errorMessage)
        else if (httpStatus?.equals(HttpStatus.SERVICE_UNAVAILABLE) == true)
            HttpResponse.serverError(errorMessage)
        else if (httpStatus?.equals(HttpStatus.INTERNAL_SERVER_ERROR) == true)
            HttpResponse.serverError(errorMessage)
        else if (httpStatus?.equals(HttpStatus.NOT_FOUND) == true)
            HttpResponse.notFound(errorMessage)
        else
            HttpResponse.serverError(errorMessage)
    }


    @Error(status = HttpStatus.NOT_FOUND, global = true)
    fun notFound(request: HttpRequest<*>?): HttpResponse<ErrorResponse> {
        return HttpResponse.notFound(
            ErrorResponse(
                AresError.ERR_1008.code,
                AresError.ERR_1008.message,
                HttpStatus.NOT_FOUND,
                null
            )
        )
    }

}