package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.AccountPayableFileResponse
import com.cogoport.ares.api.payment.model.AccountPayablesFile

interface KnockoffService {
    suspend fun uploadBillPayment(knockOffList: List<AccountPayablesFile>): MutableList<AccountPayableFileResponse>
}
