package com.cogoport.ares.api.payment.service.interfaces

import com.cogoport.ares.api.payment.model.OpenSearchListRequest
import com.cogoport.ares.api.payment.model.OpenSearchRequest
import com.cogoport.ares.model.payment.CompanyType
import com.cogoport.ares.model.payment.ServiceType
import java.util.UUID

interface OpenSearchService {

    suspend fun pushDashboardData(request: OpenSearchRequest)

    suspend fun pushOutstandingData(request: OpenSearchRequest)
    suspend fun pushOutstandingListData(request: OpenSearchListRequest)
    suspend fun generateCollectionTrend(
        zone: String?,
        quarter: Int,
        year: Int,
        serviceType: ServiceType?,
        invoiceCurrency: String?,
        defaultersOrgIds: List<UUID>?
    )

    suspend fun generateOverallStats(
        zone: String?,
        quarter: Int,
        year: Int,
        serviceType: ServiceType?,
        invoiceCurrency: String?,
        defaultersOrgIds: List<UUID>?
    )

    suspend fun generateMonthlyOutstanding(
        zone: String?,
        quarter: Int,
        year: Int,
        serviceType: ServiceType?,
        invoiceCurrency: String?,
        defaultersOrgIds: List<UUID>?
    )

    suspend fun generateQuarterlyOutstanding(
        quarter: Int,
        year: Int,
        serviceType: ServiceType?,
        defaultersOrgIds: List<UUID>?,
        cogoEntityId: UUID?,
        companyType: CompanyType?
    )

    suspend fun generateDailySalesOutstanding(
        quarter: Int,
        year: Int,
        serviceType: ServiceType?,
        date: String,
        defaultersOrgIds: List<UUID>?,
        cogoEntityId: UUID?,
        companyType: CompanyType?
    )

    suspend fun generateDailyPayableOutstanding(
        zone: String?,
        quarter: Int,
        year: Int,
        serviceType: ServiceType?,
        invoiceCurrency: String?,
        date: String,
        dashboardCurrency: String
    )


    suspend fun generateArDashboardData()

    suspend fun generateOutstandingData(
        searchKey: String,
        cogoEntityId: UUID?,
        defaultersOrgIds: List<UUID>?
    )
}
