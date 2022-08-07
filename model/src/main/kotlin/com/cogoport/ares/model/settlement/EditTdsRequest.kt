package com.cogoport.ares.model.settlement

import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID
import javax.validation.constraints.NotNull
@Introspected
data class EditTdsRequest(
    @field: NotNull(message = "documentNo is required") var documentNo: String?,
    @field: NotNull(message = "settlementType is required") val settlementType: SettlementType?,
    @field: NotNull(message = "oldTds is required") val oldTds: BigDecimal?,
    @field: NotNull(message = "newTds is required") val newTds: BigDecimal?,
    @field: NotNull(message = "exchangeRate is required") val exchangeRate: BigDecimal?,
    @field: NotNull(message = "currency is required") val currency: String?,
    @field: NotNull(message = "Updated By is required") val updatedBy: UUID?,
    val updatedByUserType: String?

)
