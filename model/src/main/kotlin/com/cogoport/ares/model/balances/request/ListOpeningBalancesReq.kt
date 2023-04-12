package com.cogoport.ares.model.balances.request

import com.cogoport.ares.model.common.Pagination
import io.micronaut.core.annotation.Introspected
import java.time.LocalDate
import javax.validation.constraints.NotNull

@Introspected
data class ListOpeningBalancesReq(
    var q: String? = null,
    @field: NotNull
    var entityCode: Int? = null,
    @field: NotNull
    var date: LocalDate? = null,
    var sortField: String = "balanceAmount",
    var sortType: String = "DESC"
) : Pagination()
