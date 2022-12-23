package com.cogoport.ares.model.settlement.event

import com.cogoport.ares.model.settlement.SettlementType
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import java.sql.Timestamp

@Introspected
@MappedEntity
class PaymentInvoiceInfo(
    var documentValue: String? = null,
    var settlementDate: Timestamp?,
    var sourceType: SettlementType?,
    var sourceId: Int?
)
