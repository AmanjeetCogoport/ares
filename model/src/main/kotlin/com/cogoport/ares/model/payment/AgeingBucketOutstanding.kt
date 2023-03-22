package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
@ReflectiveAccess
data class AgeingBucketOutstanding(
    @JsonProperty("ledgerAmount")
    var ledgerAmount: BigDecimal?,
    @JsonProperty("ledgerCount")
    var ledgerCount: Int?,
    @JsonProperty("ledgerCurrency")
    var ledgerCurrency: String?,
    @JsonProperty("invoiceBucket")
    var invoiceBucket: MutableList<DueAmount>
)
