package com.cogoport.ares.model.dunning.response

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreType
data class OrganizationCreditLimitDetailResponse(
    @JsonProperty("credit_applicable")
    val creditApplicable: Boolean,
    @JsonProperty("mode_type")
    var modeType: String?,
    @JsonProperty("credit_limit")
    var creditLimit: BigDecimal?,
    @JsonProperty("credit_limit_currency")
    var creditLimitCurrency: String?,
    @JsonProperty("validity_start_date")
    var validityStartDate: Timestamp?,
    @JsonProperty("credit_cycle_id")
    var creditCycleId: UUID?,
    @JsonProperty("interest")
    var interest: BigDecimal?,
    @JsonProperty("max_credit_days")
    var maxCreditDays: Long?,
    @JsonProperty("credit_source")
    var creditSource: String
)
