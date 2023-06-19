package com.cogoport.ares.api.sage.service.interfaces

import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.SagePaymentDetails

interface SageService {
    suspend fun checkIfDocumentExistInSage(documentValue: String, sageBPRNumber: String, organizationSerialId: Long?, documentType: AccountType, registrationNumber: String?): String?

    suspend fun sagePaymentBySageRefNumbers(paymentNumValue: ArrayList<String?>): ArrayList<SagePaymentDetails>
}
