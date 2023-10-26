package com.cogoport.ares.model.payment.request

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class UpdateAccountTaggingRequest(
    val organizationId: UUID?,
    val entityCode: Int?,
    val taggedState: String?
)
