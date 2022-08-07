package com.cogoport.ares.model.payment.event

import java.util.UUID

data class DeleteInvoiceRequest(
    var data: MutableList<Pair<Long, String>>,
    var performedBy: UUID?,
    var performedByUserType: String?
)
