package com.cogoport.ares.model.settlement.event

import com.cogoport.ares.model.settlement.SettlementType
import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.sql.Date
import java.sql.Timestamp

@Introspected
@MappedEntity
class PaymentInfoRec(
    var documentNumber: String? = null,
    var settlementDate: String?,
    var sourceType: SettlementType?,
    var sourceId: Int?
)
