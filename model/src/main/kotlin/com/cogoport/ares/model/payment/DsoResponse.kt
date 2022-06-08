package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class DsoResponse(
    @JsonProperty("month")
    val month: String,
    @JsonProperty("dsoForTheMonth")
    val dsoForTheMonth: BigDecimal
)
