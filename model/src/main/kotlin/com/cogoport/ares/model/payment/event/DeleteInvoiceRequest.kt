package com.cogoport.ares.model.payment.event

import java.util.*

data class DeleteInvoiceRequest(
    var data: MutableList<Pair<Long, String>>,
    var performedBy: UUID?,
    var performedByUserType: String?
)
