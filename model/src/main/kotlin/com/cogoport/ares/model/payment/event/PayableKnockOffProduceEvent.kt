package com.cogoport.ares.model.payment.event

import com.cogoport.ares.model.payment.AccountPayableFileResponse
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class PayableKnockOffProduceEvent(
    var accountPayableFileResponse: AccountPayableFileResponse
)
