package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.ServiceType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.http.annotation.QueryValue

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class OverallStatsRequest(
    @QueryValue(AresModelConstants.ZONE) val zone: String?,
    @QueryValue(AresModelConstants.ROLE) val role: String?,
    @QueryValue("service_type") val serviceType: ServiceType?,
    @QueryValue("currency_type") val currencyType: String?
)
