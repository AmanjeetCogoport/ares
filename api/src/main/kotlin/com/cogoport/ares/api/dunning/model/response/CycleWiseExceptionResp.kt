package com.cogoport.ares.api.dunning.model.response

import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.util.UUID

@MappedEntity
data class CycleWiseExceptionResp(
    var tradePartyName: String,
    var registrationNumber: String,
    var tradePartyDetailId: UUID,
    var totalOutstanding: BigDecimal,
    var totalOnAccount: BigDecimal,
    var currency: String
)
