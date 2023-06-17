package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.common.Pagination
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID

@Introspected
data class DunningCycleFilterRequest(
    val query: String?,
    val entityCode: Int,
    val ageingStartDay: Int,
    val ageingLastDay: Int,
    var serviceTypes: List<ServiceType>?,
    var taggedOrganizationIds: List<UUID>?,
    val totalDueOutstanding: BigDecimal?,
    val exceptionTradePartyDetailId: List<UUID>?,
    val pageSizeData: Int?,
    val pageIndexData: Int?
) : Pagination()
