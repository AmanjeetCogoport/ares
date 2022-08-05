package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected

@Introspected
data class CogoOrganizationRequest(
    var organizationId: String?,
    var organizationSerialId: Long?,
)
