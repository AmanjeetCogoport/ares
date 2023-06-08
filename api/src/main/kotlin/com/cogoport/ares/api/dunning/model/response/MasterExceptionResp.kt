package com.cogoport.ares.api.dunning.model.response

import com.cogoport.ares.api.dunning.model.OrgSegmentation
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal

@MappedEntity
data class MasterExceptionResp(
    val name: String,
    val registrationNumber: String,
    val segmentation: OrgSegmentation?,
    val creditDays: Long?,
    val creditAmount: BigDecimal?,
    val totalDueAmount: BigDecimal?
)
