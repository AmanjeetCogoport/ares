package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.payment.ServiceType
import java.math.BigDecimal
import java.util.UUID

data class DunningCycleFilterRequest(
    val entityCode: Int,
    val ageingStartDay: Int,
    val ageingLastDay: Int,
    var serviceTypes: List<ServiceType>?,
    var taggedOrganizationIds: List<UUID>?,
    val totalDueOutstanding: BigDecimal?,
    val exceptionTradePartyDetailId: List<UUID>?
)
