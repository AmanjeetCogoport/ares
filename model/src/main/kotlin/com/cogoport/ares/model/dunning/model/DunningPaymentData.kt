package com.cogoport.ares.model.dunning.model

import com.cogoport.heimdall.model.common.PaymentTriggerHandlerReq
import com.cogoport.heimdall.model.payment.response.GetPaymentResponse
import io.micronaut.core.annotation.Introspected

@Introspected
data class DunningPaymentData(
    var paymentResponse: PaymentTriggerHandlerReq<GetPaymentResponse>?
)
