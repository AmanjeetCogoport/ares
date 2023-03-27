package com.cogoport.ares.api.payment.model.response

import com.cogoport.ares.api.payment.entity.BfReceivableAndPayable
import io.micronaut.core.annotation.Introspected

@Introspected
data class ServiceWiseOverdueResp(
    var arData: BfReceivableAndPayable,
    var apData: BfReceivableAndPayable,
)
