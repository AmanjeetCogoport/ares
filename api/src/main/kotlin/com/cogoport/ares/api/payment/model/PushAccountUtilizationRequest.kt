package com.cogoport.ares.api.payment.model

import com.cogoport.ares.model.payment.DocumentSearchType
import io.micronaut.core.annotation.Introspected

@Introspected
data class PushAccountUtilizationRequest(
    var inputType: DocumentSearchType,
    var accountUtilizations: List<DocIdAccTypePair>
)
