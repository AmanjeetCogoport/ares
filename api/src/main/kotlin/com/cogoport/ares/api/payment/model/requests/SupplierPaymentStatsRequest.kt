package com.cogoport.ares.api.payment.model.requests

import com.fasterxml.jackson.annotation.JsonFormat
import io.micronaut.core.annotation.Introspected

@Introspected
data class SupplierPaymentStatsRequest(
        var orgId: String,
        @JsonFormat(pattern = "yyyy-MM-dd")
        var startDate: String? = null,
        @JsonFormat(pattern = "yyyy-MM-dd")
        var endDate: String? = null,
        var timePeriod: String? = null,
        var entityCode: Int? = null,
        var currency: String? = null
)
