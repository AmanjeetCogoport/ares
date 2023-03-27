package com.cogoport.ares.api.common.models

import com.cogoport.ares.api.payment.entity.Outstanding
import io.micronaut.core.annotation.Introspected

@Introspected
data class DailyStatsResponse(
    var salesInvoiceResponse: ArrayList<Outstanding>,
    var creditNoteResponse: ArrayList<Outstanding>?,
    var onAccountPaymentResponse: ArrayList<Outstanding>?,
    var shipmentCreated: ArrayList<Outstanding>?
)
