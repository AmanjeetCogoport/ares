package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccMode
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID

@Introspected
data class JvLineItemResponse(
    var id: String?,
    var entityCode: Int?,
    var accMode: AccMode?,
    var tradePartyId: UUID?,
    var glCode: String?,
    var tradePartyName: String?,
    var type: String?,
    var amount: BigDecimal,
    var ledAmount: BigDecimal,
    var parentId: String,
    var entityId: UUID?,
    var currency: String?,
    var ledCurrency: String?,
    var description: String?,
    var category: String?
)
