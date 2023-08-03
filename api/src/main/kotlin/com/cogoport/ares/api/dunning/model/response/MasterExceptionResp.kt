package com.cogoport.ares.api.dunning.model.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@Introspected
@MappedEntity
data class MasterExceptionResp(
    var id: String,
    val name: String,
    val isActive: Boolean,
    val registrationNumber: String,
    val orgSegment: String?,
    val entityCode: Long,
    val creditDays: Long?,
    val creditAmount: BigDecimal?,
    val totalDueAmount: BigDecimal?,
    val currency: String?
)
