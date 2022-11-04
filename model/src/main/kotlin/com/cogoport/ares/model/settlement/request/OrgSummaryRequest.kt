package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.payment.AccMode
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class OrgSummaryRequest(
    @field:NotNull(message = "orgId is mandatory")
    val orgId: UUID?,
    @field:NotNull(message = "accMode is mandatory")
    val accMode: AccMode?,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null
)
