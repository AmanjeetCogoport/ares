package com.cogoport.ares.model.payment

import com.cogoport.ares.model.common.AresModelConstants
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.http.annotation.QueryValue

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class DsoRequest(
    @QueryValue(AresModelConstants.ZONE) val zone: String? = null,
    @QueryValue(AresModelConstants.ROLE) val role: String? = null,
    @QueryValue(AresModelConstants.QUARTER_YEAR) val quarterYear: List<String> = listOf("Q" + AresModelConstants.CURR_QUARTER + "_" + AresModelConstants.CURR_YEAR),
    @QueryValue("service_type") val serviceType: ServiceType?,
    @QueryValue("currency_type") val currencyType: String?,
    @QueryValue("invoice_currency") val invoiceCurrency: String?
)
