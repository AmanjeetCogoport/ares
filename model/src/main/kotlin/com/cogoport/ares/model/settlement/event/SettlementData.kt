package com.cogoport.ares.model.settlement.event

import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
class SettlementData(
    var sourceId: Int?,
    var sourceType: SettlementType?,
)
