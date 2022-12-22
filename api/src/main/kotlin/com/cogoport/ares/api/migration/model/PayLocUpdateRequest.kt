package com.cogoport.ares.api.migration.model

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class PayLocUpdateRequest(
    val sageOrganizationId: String?,
    val documentValue: String?,
    val amtLoc: BigDecimal?,
    val payCurr: BigDecimal?,
    val payLoc: BigDecimal?,
)
