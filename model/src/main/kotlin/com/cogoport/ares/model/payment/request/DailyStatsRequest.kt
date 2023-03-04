package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.payment.CompanyType
import com.cogoport.ares.model.payment.DocumentType
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import java.util.UUID

@Introspected
data class DailyStatsRequest(
    @QueryValue("month") val month: String? = null,
    @QueryValue("year") val year: Int? = null,
    @QueryValue("asOnDate") val asOnDate: String? = null,
    @QueryValue("documentType") val documentType: DocumentType? = DocumentType.SALES_INVOICE,
    @QueryValue("companyType") val companyType: CompanyType? = null,
    @QueryValue("cogoEntityId") val cogoEntityId: UUID? = null,
    @QueryValue("serviceType") val serviceType: ServiceType? = null,
    @QueryValue("dashboardCurrency") val dashboardCurrency: String? = "INR"
)
