package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.ServiceType
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
data class CollectionTrendResponse(
    @JsonProperty("duration")
    var duration: String?,
    @JsonProperty("receivableAmount")
    var receivableAmount: BigDecimal,
    @JsonProperty("collectableAmount")
    var collectableAmount: BigDecimal,
    @JsonProperty("serviceType")
    var serviceType: ServiceType?,
    @JsonProperty("dashboardCurrency")
    var dashboardCurrency: String?,
    @JsonProperty("invoiceCurrency")
    var invoiceCurrency: String?
)
