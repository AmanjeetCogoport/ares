package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.KamPaymentService
import com.cogoport.ares.model.payment.response.OverallStatsForCustomerResponse
import com.cogoport.ares.model.payment.response.OverallStatsForKamResponse
import com.cogoport.ares.model.payment.response.OverdueInvoicesResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class KamPaymentServiceImpl : KamPaymentService {
    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    override suspend fun getOverallStatsForKam(docValue: List<String>): OverallStatsForKamResponse {
        val profromaInvoices = accountUtilizationRepository.getProformaInvoicesStats(docValue)
        val duePayment = accountUtilizationRepository.getDuePayment(docValue)
        val overdueInvoice = accountUtilizationRepository.getOverdueInvoicesStats(docValue)
        val totalReceivables = accountUtilizationRepository.getTotalReceivables(docValue)

        return OverallStatsForKamResponse(
            proformaInvoices = profromaInvoices,
            dueForPayment = duePayment,
            overdueInvoices = overdueInvoice,
            totalReceivables = totalReceivables
        )
    }

    override suspend fun getOverdueInvoicesByDueDateForKam(docValue: List<String>): OverdueInvoicesResponse {
        val response = accountUtilizationRepository.getOverdueInvoices(docValue)
        return response
    }

    override suspend fun getOverallStatsForCustomer(
        docValue: List<String>,
        custId: String
    ): OverallStatsForCustomerResponse {
        val profromaInvoices = accountUtilizationRepository.getProformaInvoicesForCustomer(docValue, custId)
        val duePayment = accountUtilizationRepository.getDuePaymentForCustomer(docValue, custId)
        val overdueInvoice = accountUtilizationRepository.getOverdueInvoicesForCustomer(docValue, custId)
        val totalReceivables = accountUtilizationRepository.getTotalReceivablesForCustomer(docValue, custId)
        val onAccountPayment = accountUtilizationRepository.getOnAccountPaymentForCustomer(docValue, custId)

        return OverallStatsForCustomerResponse(
            proformaInvoices = profromaInvoices,
            dueForPayment = duePayment,
            overdueInvoices = overdueInvoice,
            totalReceivables = totalReceivables,
            onAccountPayment = onAccountPayment
            )
    }

    override suspend fun getOverdueInvoicesByDueDateForCustomer(
        docValue: List<String>,
        custId: String
    ): OverdueInvoicesResponse {
        val response = accountUtilizationRepository.getOverdueInvoicesByDueDateForCustomer(docValue, custId)
        return response
    }

}