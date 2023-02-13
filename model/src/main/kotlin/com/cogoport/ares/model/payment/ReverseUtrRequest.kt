package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class ReverseUtrRequest(
    val documentNo: Long,
    var transactionRef: String,
    var updatedBy: UUID?,
    var performedByType: String?,
    var paymentUploadAuditId: List<Long>,
    var knockOffType: String? = null
)
