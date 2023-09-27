package com.cogoport.ares.model.settlement.request

import com.cogoport.ares.model.payment.AccMode
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.Date
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
data class JvLineItemRequest(
    var id: String?,
    var entityCode: Int?,
    var entityId: UUID?,
    var accMode: AccMode?,
    @field:NotNull(message = "glCode is mandatory")
    var glCode: String,
    var tradePartyId: UUID?,
    var tradePartyName: String?,
    @field:NotNull(message = "Type is mandatory")
    var type: String,
    var amount: BigDecimal,
    var validityDate: Date?
)