package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotNull

@Introspected
data class AccPayablesOfOrgReq(

    @field:NotNull(message = "Organization Id can't be null")
    var orgId: String,
    var entityCode: Int? = null
)
