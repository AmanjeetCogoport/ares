package com.cogoport.ares.model.sage

import io.micronaut.core.annotation.Introspected

@Introspected
data class SageOrganizationAccountTypeRequest(
    var organizationSerialId: String,
    var accountType: String
)
