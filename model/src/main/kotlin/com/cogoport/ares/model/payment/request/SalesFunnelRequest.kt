package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.payment.CompanyType
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue

@Introspected
data class SalesFunnelRequest(
    @QueryValue("month") val month: String? = null,
    @QueryValue("entityCode") var entityCode: Int? = 301,
    @QueryValue("companyType") val companyType: CompanyType? = null,
    @QueryValue("serviceType") val serviceType: ServiceType? = null,
)
