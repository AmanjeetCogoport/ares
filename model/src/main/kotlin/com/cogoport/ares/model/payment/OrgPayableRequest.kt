package com.cogoport.ares.model.payment

import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
@Introspected

data class OrgPayableRequest(
    @QueryValue("orgId") val orgId: String?
)
