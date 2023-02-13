package com.cogoport.ares.model.payment

import com.cogoport.ares.model.common.KnockOffStatus
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class RestoreUtrResponse(
    val documentNo: Long,
    var paidAmount: BigDecimal,
    var paidTds: BigDecimal,
    var paymentStatus: KnockOffStatus,
    var paymentUploadAuditId: List<Long>,
    var updatedBy: UUID?,
    var performedByType: String?,
    var settlementIds: List<Long>
)
