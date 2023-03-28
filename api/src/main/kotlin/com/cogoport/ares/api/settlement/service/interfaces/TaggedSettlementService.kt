package com.cogoport.ares.api.settlement.service.interfaces

import com.cogoport.ares.model.payment.request.OnAccountPaymentRequest

interface TaggedSettlementService {

    suspend fun settleOnAccountInvoicePayment(req: OnAccountPaymentRequest)
}
