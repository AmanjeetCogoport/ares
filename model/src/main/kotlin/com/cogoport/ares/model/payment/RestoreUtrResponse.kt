package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class RestoreUtrResponse(
    val documentNo: Long,
    var currencyAmount: BigDecimal,
    var tdsAmount: BigDecimal,
    var ledgerAmount: BigDecimal,
    var ledgerTdsAmount: BigDecimal
)
