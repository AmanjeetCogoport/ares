package com.cogoport.ares.api.payment.model.response

import com.cogoport.ares.api.payment.entity.BfReceivableAndPayable
import com.cogoport.ares.api.payment.entity.ServiceWiseCardData
import io.micronaut.core.annotation.Introspected

@Introspected
data class ServiceWiseOverdueResp(
    var arData: BfReceivableAndPayable,
    var apData: BfReceivableAndPayable,
    var cardDataAr: ServiceWiseCardData,
    var cardDataAp: ServiceWiseCardData
)
