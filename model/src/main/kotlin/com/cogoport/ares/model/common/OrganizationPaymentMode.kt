package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.HashMap
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
data class OrganizationPaymentMode(
    var id: UUID?,
    @JsonProperty("organization_id")
    var organizationId: UUID?,
    @JsonProperty("organization_trade_party_id")
    var organizationTradePartyId: UUID?,
    var mode: String?,
    @JsonProperty("mode_type")
    var modeType: String?,
    @JsonProperty("validity_start_date")
    var validityStartDate: String?,
    @JsonProperty("validity_end_date")
    var validityEndDate: String?,
    @JsonProperty("credit_limit")
    var creditLimit: BigDecimal?,
    @JsonProperty("credit_limit_currency")
    var creditLimitCurrency: String?,
    @JsonProperty("interest_rate_details")
    var interestRateDetails: Any?,
    var status: String?,
    @JsonProperty("created_at")
    var createdAt: String?,
    @JsonProperty("updated_at")
    var updatedAt: String?,
    @JsonProperty("mode_source")
    var modeSource: String?,
    @JsonProperty("interest_rate_type_in_days")
    var interestRateTypeInDays: Int?,
    @JsonProperty("free_credit_days")
    var freeCreditDays: Int?,
    @JsonProperty("interest_rate_on_overdue")
    var interestRateOnOverdue: Int?,
    @JsonProperty("interest_rate_type")
    var interestRateType: String?,
    @JsonProperty("terms_and_condition_id")
    var termsAndConditionId: String?,
    @JsonProperty("organizationDocumentId")
    var organizationDocumentId: String?,
    @JsonProperty("is_offline")
    var isOffline: Boolean?,
    @JsonProperty("organization_trade_party")
    var organizationTradeParty: HashMap<Any?, Any?>?,
    var organization: HashMap<Any?, Any?>?
)
