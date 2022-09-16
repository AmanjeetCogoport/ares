package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementHistoryRequest(
    @field:NotNull(message = "orgId is mandate")
    val orgId: List<UUID>? = null,
    @field:NotNull(message = "accountType is mandate")
    val accountType: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val page: Int = 1,
    val pageLimit: Int = 10,
    val query: String = "",
)
