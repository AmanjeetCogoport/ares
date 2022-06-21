package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonInclude
@JsonInclude
data class OrgPayableResponse(
    val totalReceivables: PayableOutstandingResponse,
    val collectionTrend: List<OutstandingResponse>,
    val ageingBucket: List<PayableAgeingBucket>? = null
)
