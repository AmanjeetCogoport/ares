package com.cogoport.ares.model.settlement.event

import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@Introspected
@MappedEntity
class PaymentInfoRec(
    var transRefNumber: String? = null,
    var settlementDate: Timestamp?,
    var sourceType: SettlementType?,
    var sourceId: Int?
)
