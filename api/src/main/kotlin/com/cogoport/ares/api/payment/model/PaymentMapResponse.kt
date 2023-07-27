package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.PaymentInvoiceMappingType
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class PaymentMapResponse(
    var id: Long,
    var mappingType: PaymentInvoiceMappingType,
    var amount: BigDecimal,
    var ledAmount: BigDecimal
)
