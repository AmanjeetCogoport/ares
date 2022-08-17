package com.cogoport.ares.model.payment

import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class AccountUtilizationEvent(val accUtilizationRequest: AccUtilizationRequest)
