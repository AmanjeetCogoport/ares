package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class ReverseUtrRequest(
    val documentNo: Long,
    var updatedBy: UUID?,
    var performedByType: String?,
    var currencyAmount: BigDecimal,
    var tdsAmount: BigDecimal,
    var ledgerAmount: BigDecimal,
    var ledgerTdsAmount: BigDecimal
)
