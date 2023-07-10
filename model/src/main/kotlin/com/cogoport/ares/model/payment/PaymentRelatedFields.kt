package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@MappedEntity
data class PaymentRelatedFields(
    @JsonProperty("payment_num")
    val paymentNum: Long?,
    @JsonProperty("payment_num_value")
    val paymentNumValue: String?,
    @JsonProperty("payment_code")
    val paymentCode: PaymentCode?
)
