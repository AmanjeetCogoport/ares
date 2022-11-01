package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
@JsonInclude
data class CogoBanksDetails(
    @JsonProperty("id")
    var id: UUID,
    @JsonProperty("entity_code")
    var entityCode: Int,
    @JsonProperty("ledger_currency")
    var ledgerCurrency: String?,
    @JsonProperty("bank_details")
    var bankDetails: List<BankDetails>? = null
)
