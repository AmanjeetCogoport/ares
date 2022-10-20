package com.cogoport.ares.model.payment

import com.cogoport.ares.model.payment.response.DpoResponse
import com.cogoport.ares.model.payment.response.DsoResponse
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DailySalesOutstanding(
    @JsonProperty("averageDsoForTheMonth")
    val averageDsoForTheMonth: BigDecimal,
    @JsonProperty("averageDsoLast3Months")
    val averageDsoLast3Months: BigDecimal,
    @JsonProperty("dsoResponse")
    val dsoResponse: List<DsoResponse>,
    @JsonProperty("dpoResponse")
    val dpoResponse: List<DpoResponse>,
    @JsonProperty("serviceType")
    val serviceType: String?,
    @JsonProperty("currencyType")
    val currencyType: String?
)
