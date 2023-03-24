package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class BfShipmentProfitabilityResp(
    var jobNumber: String,
    var shipmentType: String,
    var businessName: String,
    var entity: String,
    var shipmentMilestone: String,
    var income: BigDecimal,
    var expense: BigDecimal,
    var profitability: BigDecimal,
    var jobStatus: String
)
