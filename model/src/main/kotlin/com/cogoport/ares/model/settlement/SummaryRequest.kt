package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.payment.AccMode
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import java.sql.Timestamp
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SummaryRequest(
    @field:NotNull(message = "entityCode cannot be null")
    var entityCode: Int?,
    @QueryValue(AresModelConstants.ORG_ID) val orgId: UUID,
    @QueryValue(AresModelConstants.ACC_MODE) val accModes: List<AccMode>?,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null
)
