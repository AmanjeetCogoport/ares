package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.model.payment.AccountPayablesFile
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.response.AccountPayableFileResponse

interface KnockoffService {
    suspend fun uploadBillPayment(knockOffRecord: AccountPayablesFile): AccountPayableFileResponse

    suspend fun reverseUtr(documentNo: Long,accountType: AccountType)
}
