package com.cogoport.ares.api.payment.service.implementation

import com.cogoport.ares.api.payment.repository.AccountUtilizationRepository
import com.cogoport.ares.api.payment.service.interfaces.KamPaymentService
import com.cogoport.ares.model.payment.OutstandingList
import com.cogoport.ares.model.payment.response.OverallStatsForKamResponse
import com.cogoport.ares.model.payment.response.OverdueInvoicesResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.ceil

@Singleton
class KamPaymentServiceImpl : KamPaymentService {
    @Inject
    lateinit var accountUtilizationRepository: AccountUtilizationRepository

    override suspend fun getProformaInvoicesForKam(proformaIds: List<String>): OverallStatsForKamResponse {
        val response = accountUtilizationRepository.getProformaInvoicesStats(proformaIds)
        return OverallStatsForKamResponse(
            totalAmount = response.TotalAmount,
            invoicesCount = response.InvoicesCount,
            customersCount = response.CustomersCount
        )
    }

    override suspend fun getDueForPaymentForKam(proformaIds: List<String>): OverallStatsForKamResponse {
        val response = accountUtilizationRepository.getDuePayment(proformaIds)
        return OverallStatsForKamResponse(
            totalAmount = response.TotalAmount,
            invoicesCount = response.InvoicesCount,
            customersCount = response.CustomersCount
        )
    }

    override suspend fun getOverdueInvoicesForKam(proformaIds: List<String>): OverallStatsForKamResponse {
        val response = accountUtilizationRepository.getOverdueInvoicesStats(proformaIds)
        return OverallStatsForKamResponse(
            totalAmount = response.TotalAmount,
            invoicesCount = response.InvoicesCount,
            customersCount = response.CustomersCount
        )
    }

    override suspend fun getTotalReceivablesForKam(proformaIds: List<String>): OverallStatsForKamResponse {
        val response = accountUtilizationRepository.getTotalReceivables(proformaIds)
        return OverallStatsForKamResponse(
            totalAmount = response.TotalAmount,
            invoicesCount = response.InvoicesCount,
            customersCount = response.CustomersCount
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

}