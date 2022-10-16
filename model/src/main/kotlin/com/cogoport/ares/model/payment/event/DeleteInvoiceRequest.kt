package com.cogoport.ares.model.payment.event

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class DeleteInvoiceRequest(
    var data: MutableList<Pair<Long, String>>,
    var performedBy: UUID?,
    var performedByUserType: String?
)
