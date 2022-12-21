package com.cogoport.ares.api.settlement.model

import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.sql.Timestamp

@Introspected
@MappedEntity
class PaymentInvoiceInfo(
    var transRefNumber: String? = null,
    var settlementDate: Timestamp?,
    var sourceType: SettlementType?,
    var sourceId: Int?
)
