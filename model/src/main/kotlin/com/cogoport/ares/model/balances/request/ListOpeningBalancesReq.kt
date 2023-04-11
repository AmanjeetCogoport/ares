package com.cogoport.ares.model.balances.request

import com.cogoport.ares.model.common.Pagination
import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import java.util.Date
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class ListOpeningBalancesReq(
    var q: String?,
    @field: NotNull
    var entityCode: UUID?,
    @field: NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    var date: Date?
) : Pagination()
