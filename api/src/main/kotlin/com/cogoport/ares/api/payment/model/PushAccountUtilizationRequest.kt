package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.AccountUtilizationId
import io.micronaut.core.annotation.Introspected

@Introspected
data class PushAccountUtilizationRequest(
    var inputType: AccountUtilizationId,
    var accountUtilizations: List<DocIdAccTypePair>
)
