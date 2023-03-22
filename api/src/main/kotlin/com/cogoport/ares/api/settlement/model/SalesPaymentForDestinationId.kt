package com.cogoport.ares.api.settlement.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.math.BigDecimal

@Introspected
@MappedEntity
data class SalesPaymentForDestinationId(
    var documentValue: String,
    var orgSerialId: Long,
    @field: TypeDef(type = DataType.JSON)
    @JsonProperty("payment_values")
    var paymentValues: String?,
    var totalAmount: BigDecimal?,

)
