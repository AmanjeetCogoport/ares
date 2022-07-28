package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.models.ExchangeRequest
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.ExchangeClient
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import com.cogoport.ares.model.payment.InvoiceType
import com.cogoport.ares.model.settlement.SettlementType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal

@Singleton
class SettlementServiceHelper {

    @Inject
    lateinit var exchangeClient: ExchangeClient

    fun getInvoiceType(accType: AccountType): String {
        return when (accType) {
            AccountType.SINV -> {
                InvoiceType.SINV.value
            }
            AccountType.SCN -> {
                InvoiceType.SCN.value
            }
            AccountType.SDN -> {
                InvoiceType.SDN.value
            }
            AccountType.REC -> {
                InvoiceType.REC.value
            }
            AccountType.PINV -> {
                InvoiceType.PINV.value
            }
            AccountType.PCN -> {
                InvoiceType.PCN.value
            }
            AccountType.PDN -> {
                InvoiceType.PDN.value
            }
            AccountType.PAY -> {
                InvoiceType.PAY.value
            }
        }
    }

    fun getDocumentStatus(afterTdsAmount: BigDecimal, balanceAmount: BigDecimal, docType: SettlementType): String {
        val payments = listOf(SettlementType.REC, SettlementType.PAY, SettlementType.SCN, SettlementType.PCN)
        return if (balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
            if (payments.contains(docType)) DocStatus.UTILIZED.value else DocStatus.PAID.value
        } else if (afterTdsAmount.compareTo(balanceAmount) != 0) {
            if (payments.contains(docType)) DocStatus.PARTIAL_UTILIZED.value else DocStatus.PARTIAL_PAID.value
        } else if (afterTdsAmount.compareTo(balanceAmount) == 0) {
            if (payments.contains(docType)) DocStatus.UNUTILIZED.value else DocStatus.UNPAID.value
        } else {
            throw AresException(AresError.ERR_1504, "")
        }
    }

    /**
     * Get Exchange Rate from AWS Lambda.
     * @param: from
     * @param: to
     * @param: transactionDate
     * @return: BgDecimal
     */
    suspend fun getExchangeRate(from: String, to: String, transactionDate: String): BigDecimal {
        try {
            return exchangeClient.getExchangeRate(ExchangeRequest(from, to, transactionDate)).exchangeRate
        } catch (e: Exception) {
            logger().error("Exchange Rate not found in for {} to {} for date: {}", from, to, transactionDate)
            throw AresException(AresError.ERR_1505, "$from to $to")
        }
    }
}
