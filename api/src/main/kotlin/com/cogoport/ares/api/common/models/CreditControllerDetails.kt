package com.cogoport.ares.api.common.models

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class CreditControllerDetails(
    var name: String? = null,
    var email: String? = null,
    var mobileCountryCode: String? = null,
    var mobileNumber: String? = null
)
