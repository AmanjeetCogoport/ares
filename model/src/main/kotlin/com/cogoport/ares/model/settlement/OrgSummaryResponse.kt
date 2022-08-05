package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID
@Introspected
@JsonInclude
data class OrgSummaryResponse(
    val orgId: UUID,
    val orgName: String,
    val outstanding: BigDecimal,
    val currency: String,
    var tdsStyle: TdsStyle?,
)
