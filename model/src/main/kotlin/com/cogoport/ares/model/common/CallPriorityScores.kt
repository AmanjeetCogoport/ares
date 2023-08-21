package com.cogoport.ares.model.common

import io.micronaut.core.annotation.Introspected

@Introspected
data class CallPriorityScores(
    var outstandingScore: Int? = 0,
    var ageingBucketScore: Int? = 0,
    var businessContinuityScore: Int? = 0,
    var overduePerTotalAmount: Int? = 0,
    var paymentHistoryScore: Int? = 0
) {
    fun geTotalCallPriority() =
        "${this.outstandingScore!!}${this.ageingBucketScore!!}${this.businessContinuityScore!!}${this.overduePerTotalAmount!!}${this.paymentHistoryScore!!}".toInt()
}
