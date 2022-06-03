package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.ReflectiveAccess

@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AccountPayableFileResponse(
    var documentNo: Long,
    var documentValue: String,
    var isSuccess: Boolean,
    var failureReason: String?
)
