package com.cogoport.ares.model.payment.request

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.CompanyType
import com.cogoport.ares.model.payment.ServiceType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.http.annotation.QueryValue
import java.util.UUID

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class OutstandingAgeingRequest(
    @QueryValue(AresModelConstants.SERVICE_TYPE) val serviceType: ServiceType?,
    @QueryValue(AresModelConstants.COMPANY_TYPE) val companyType: CompanyType?,
    @QueryValue(AresModelConstants.COGO_ENTITY_ID)val cogoEntityId: UUID?
)
