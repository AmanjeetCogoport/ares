package com.cogoport.ares.model.payment

import com.cogoport.ares.model.common.AresModelConstants
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.http.annotation.QueryValue
import java.util.*

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class DsoRequest(
    @QueryValue(AresModelConstants.ROLE) val role: String? = null,
    @QueryValue(AresModelConstants.SERVICE_TYPE) val serviceType: ServiceType?,
    @QueryValue(AresModelConstants.COGO_ENTITY_ID) val cogoEntityId: UUID? =  null,
    @QueryValue(AresModelConstants.COMPANY_TYPE) val companyType: String? = null
)
