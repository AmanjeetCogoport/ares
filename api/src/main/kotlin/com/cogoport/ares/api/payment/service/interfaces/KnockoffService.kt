package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccountPayableFileResponse
import com.cogoport.ares.model.payment.AccountPayablesFile

interface KnockoffService {
    suspend fun uploadBillPayment(knockOffRecord: AccountPayablesFile): AccountPayableFileResponse
}
