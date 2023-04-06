package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccountPayablesFile
import com.cogoport.ares.model.payment.ReverseUtrRequest
import com.cogoport.ares.model.payment.response.AccountPayableFileResponse
import com.cogoport.ares.model.settlement.event.UpdateSettlementWhenBillUpdatedEvent

interface KnockoffService {
    suspend fun uploadBillPayment(knockOffRecord: AccountPayablesFile): AccountPayableFileResponse

    suspend fun reverseUtr(reverseUtrRequest: ReverseUtrRequest)

    suspend fun editSettlementWhenBillUpdated(updateRequest: UpdateSettlementWhenBillUpdatedEvent)
}
