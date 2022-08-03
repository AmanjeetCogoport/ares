package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AccountPayableFileResponse(
    var documentNo: Long,
    var documentValue: String,
    var isSuccess: Boolean,
    var paymentStatus: String?,
    var failureReason: String?
)
