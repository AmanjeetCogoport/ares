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

    override suspend fun getOverallStatsForKam(proformaIds: List<String>): OverallStatsForKamResponse {
        val profromaInvoices = accountUtilizationRepository.getProformaInvoicesStats(proformaIds)
        val duePayment = accountUtilizationRepository.getDuePayment(proformaIds)
        val overdueInvoice = accountUtilizationRepository.getOverdueInvoicesStats(proformaIds)
        val totalReceivables = accountUtilizationRepository.getTotalReceivables(proformaIds)

        return OverallStatsForKamResponse(
            proformaInvoices = profromaInvoices,
            dueForPayment = duePayment,
            overdueInvoices = overdueInvoice,
            totalReceivables = totalReceivables
        )
    }

    override suspend fun getOverdueInvoicesByDueDateForKam(proformaIds: List<String>): OverdueInvoicesResponse {
        val response = accountUtilizationRepository.getOverdueInvoices(proformaIds)
        return OverdueInvoicesResponse(
            thirtyAmount = response.ThirtyAmount,
            sixtyAmount = response.SixtyAmount,
            ninetyAmount = response.NinetyAmount,
            ninetyPlusAmount = response.NinetyPlusAmount,
            thirtyCount = response.ThirtyCount,
            sixtyCount = response.SixtyCount,
            ninetyCount = response.NinetyCount,
            ninetyPlusCount = response.NinetyPlusCount
        )
    }

    override suspend fun getOverallStatsForCustomer(
        proformaIds: List<String>,
        custId: String
    ): OverallStatsForCustomerResponse {
        val profromaInvoices = accountUtilizationRepository.getProformaInvoicesForCustomer(proformaIds, custId)
        val duePayment = accountUtilizationRepository.getDuePaymentForCustomer(proformaIds, custId)
        val overdueInvoice = accountUtilizationRepository.getOverdueInvoicesForCustomer(proformaIds, custId)
        val totalReceivables = accountUtilizationRepository.getTotalReceivablesForCustomer(proformaIds, custId)
        val onAccountPayment = accountUtilizationRepository.getOnAccountPaymentForCustomer(proformaIds, custId)

        return OverallStatsForCustomerResponse(
            proformaInvoices = profromaInvoices,
            dueForPayment = duePayment,
            overdueInvoices = overdueInvoice,
            totalReceivables = totalReceivables,
            onAccountPayment = onAccountPayment
            )
    }

    override suspend fun getOverdueInvoicesByDueDateForCustomer(
        proformaIds: List<String>,
        custId: String
    ): OverdueInvoicesResponse {
        val response = accountUtilizationRepository.getOverdueInvoicesByDueDateForCustomer(proformaIds, custId)
        return OverdueInvoicesResponse(
            thirtyAmount = response.ThirtyAmount,
            sixtyAmount = response.SixtyAmount,
            ninetyAmount = response.NinetyAmount,
            ninetyPlusAmount = response.NinetyPlusAmount,
            thirtyCount = response.ThirtyCount,
            sixtyCount = response.SixtyCount,
            ninetyCount = response.NinetyCount,
            ninetyPlusCount = response.NinetyPlusCount
        )
    }

}