package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SummaryRequest(
    @field:NotNull(message = "entityCode cannot be null")
    val entityCode: Int?,
    @field: NotNull(message = "orgId cannot be null")
    val orgId: List<UUID>?,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null
)
