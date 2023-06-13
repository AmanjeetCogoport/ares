package com.cogoport.ares.api.dunning.model.response

import com.cogoport.ares.model.dunning.enum.OrganizationSegment
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class MasterExceptionResp(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val registrationNumber: String,
    val orgSegment: OrganizationSegment?,
    val creditDays: Long?,
    val creditAmount: BigDecimal?,
    val totalDueAmount: BigDecimal?,
)
