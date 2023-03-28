package com.cogoport.ares.api.payment.entity

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class BfShipmentProfitabilityResp(
    var jobNumber: String? = null,
    var shipmentType: String? = null,
    var businessName: String? = null,
    var taggedEntityId: String? = null,
    var shipmentMilestone: String? = null,
    var income: BigDecimal? = 0.toBigDecimal(),
    var expense: BigDecimal? = 0.toBigDecimal(),
    var profitability: BigDecimal? = 0.toBigDecimal(),
    var jobStatus: String? = null
) {
    @field:javax.persistence.Transient
    var entity: String? = null
}
