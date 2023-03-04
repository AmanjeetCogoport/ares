package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.payment.CompanyType
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import java.util.UUID

@Introspected
data class SalesFunnelRequest(
    @QueryValue("month") val month: String? = null,
    @QueryValue("cogoEntityId") val cogoEntityId: UUID? = null,
    @QueryValue("companyType") val companyType: CompanyType? = null,
    @QueryValue("serviceType") val serviceType: ServiceType? = null,
)
