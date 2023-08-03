package com.cogoport.ares.model.dunning.model

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class DunningUserInviteData(
    var userInvitationId: UUID?
)
