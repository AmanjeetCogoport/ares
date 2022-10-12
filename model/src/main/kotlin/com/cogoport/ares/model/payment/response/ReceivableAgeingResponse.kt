package com.cogoport.ares.model.payment.response

import com.cogoport.ares.model.payment.ReceivableByAgeViaServiceType
import com.cogoport.ares.model.payment.ReceivableByAgeViaZone
import com.cogoport.ares.model.payment.ServiceType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReceivableAgeingResponse(
    @JsonProperty("zone")
    var zone: List<String?>,
    @JsonProperty("serviceType")
    var serviceType: ServiceType,
    @JsonProperty("receivableByAgeViaZone")
    var receivableByAgeViaZone: MutableList<ReceivableByAgeViaZone>? = null,
    @JsonProperty("receivableByAgeViaServiceType")
    var receivableByAgeViaServiceType: MutableList<ReceivableByAgeViaServiceType>? = null
)
