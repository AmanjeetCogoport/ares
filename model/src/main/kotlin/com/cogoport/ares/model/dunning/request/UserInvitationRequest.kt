package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
data class UserInvitationRequest(
    @JsonProperty("performed_by_id")
    val performedById: String?,
    @JsonProperty("performed_by_type")
    val performedByType: String?,
    @JsonProperty("organization_id")
    val orgId: String?,
    @JsonProperty("organization_user_details")
    val orgUserDetails: MutableList<OrgUserDetail>?,
)
