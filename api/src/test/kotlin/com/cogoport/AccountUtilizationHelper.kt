package com.cogoport

import com.cogoport.ares.api.common.models.ARLedgerJobDetailsResponse
import com.cogoport.ares.model.payment.response.CreditDebitBalance
import jakarta.inject.Singleton
import java.math.BigDecimal

@Singleton
class AccountUtilizationHelper {

    fun getARLedgerJobDetailsResponse(): List<ARLedgerJobDetailsResponse> {
        return listOf(
            ARLedgerJobDetailsResponse(
                transactionDate = "2023-08-01",
                documentType = "SINV",
                documentNumber = "COGO1234",
                currency = "INR",
                amount = "15000",
                debit = 15000.toBigDecimal(),
                credit = BigDecimal.ZERO,
                transactionRefNumber = null,
                shipmentDocumentNumber = "1234qwerty",
                houseDocumentNumber = ""
            )
        )
    }
    fun getOpeningAndClosingLedger(): CreditDebitBalance {
        return CreditDebitBalance(
            ledgerCurrency = "INR",
            credit = 11345664.toBigDecimal(),
            debit = 11021867.toBigDecimal()
        )
    }
}
