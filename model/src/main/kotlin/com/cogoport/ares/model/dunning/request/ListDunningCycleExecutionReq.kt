package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.common.Pagination
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class ListDunningCycleExecutionReq(
    val dunningCycleId: String?,
    val serviceId: UUID?
) : Pagination()
