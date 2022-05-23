package com.cogoport.ares.api.exception

import com.cogoport.ares.common.models.ErrorResponse
import com.cogoport.ares.api.utils.logger
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Error
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton

@Singleton
class AresExceptionHandler : ExceptionHandler<Exception, HttpResponse<ErrorResponse>> {
    private val logger = logger()

    @Error(global = true, exception = Exception::class)
    override fun handle(request: HttpRequest<*>?, exception: Exception?): HttpResponse<ErrorResponse> {
        logger.error(request.toString(), exception)
        var errorMessage = getErrorResponse()
        if (exception is AresException) {
            errorMessage = exception?.error?.let {
                ErrorResponse(
                    it.code,
                    it.getMessage(exception.context),
                    it.httpStatus
                )
            }!!
            return getResponse(exception?.error?.httpStatus, errorMessage)
        } else {
            var errorMessage = ErrorResponse(
                AresError.ERR_1001.code,
                exception?.message,
                HttpStatus.SERVICE_UNAVAILABLE
            )
            return getResponse(HttpStatus.SERVICE_UNAVAILABLE, errorMessage)
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
        else if (httpStatus?.equals(HttpStatus.NOT_FOUND) == true)
            HttpResponse.notFound(errorMessage)
        else
            HttpResponse.serverError(errorMessage)
    }

    /**
     * Return Default ErrorResponse
     * @return ErrorResponse
     */
    private fun getErrorResponse(): ErrorResponse {
        return ErrorResponse(
            AresError.ERR_1001.code,
            AresError.ERR_1001.getMessage(""),
            AresError.ERR_1001.httpStatus
        )
    }
}
