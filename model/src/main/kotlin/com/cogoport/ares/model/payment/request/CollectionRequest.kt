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
data class CollectionRequest(
    @QueryValue(AresModelConstants.ZONE) val zone: String? = null,
    @QueryValue(AresModelConstants.ROLE) val role: String? = null,
    @QueryValue(AresModelConstants.QUARTER_YEAR) val quarterYear: String = ("Q" + AresModelConstants.CURR_QUARTER + "_" + AresModelConstants.CURR_YEAR),
    @QueryValue(AresModelConstants.SERVICE_TYPE) val serviceType: ServiceType? = null,
    @QueryValue(AresModelConstants.INVOICE_CURRENCY) val invoiceCurrency: String?,
    @QueryValue(AresModelConstants.DASHBOARD_CURRENCY) val dashboardCurrency: String? = "INR"
)
