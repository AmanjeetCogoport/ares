package com.cogoport

import com.cogoport.ares.api.common.models.InvoiceTatStatsResponse
import com.cogoport.ares.api.common.models.SalesFunnelResponse
import com.cogoport.ares.api.payment.entity.DailyOutstanding
import com.cogoport.ares.api.payment.entity.KamWiseOutstanding
import com.cogoport.ares.api.payment.entity.OverallAgeingStats
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.response.DsoResponse
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.OverallAgeingStatsResponse
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
import com.cogoport.ares.model.payment.response.QsoResponse
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.util.UUID
import kotlin.collections.LinkedHashMap

@Singleton
class DashboardHelper {
    fun getDailySalesOutstandingResponse(): MutableList<DailyOutstanding> {
        return mutableListOf(
            DailyOutstanding(
                dashboardCurrency = "INR",
                days = 31,
                month = 1,
                openInvoiceAmount = 0.toBigDecimal(),
                outstandings = 0.toBigDecimal(),
                totalSales = 0.toBigDecimal(),
                value = 0.0
            )
        )
    }

    fun getDailyOutstanding(): DailySalesOutstanding {
        val monthList = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        return DailySalesOutstanding(
            dsoResponse = monthList.map {
                DsoResponse(
                    month = it,
                    dsoForTheMonth = 0.toBigDecimal(),
                    currency = "INR"
                )
            }
        )
    }

    fun getQuarterlyOutstanding(): QuarterlyOutstanding {
        val quarterList = listOf("JAN - MAR", "APR - JUN", "JUL - SEP", "OCT - DEC")
        return QuarterlyOutstanding(
            list = quarterList.map {
                QsoResponse(
                    currency = "INR",
                    qsoForQuarter = BigDecimal(0),
                    quarter = it
                )
            }
        )
    }

    fun getOutstandingByAge(): List<OverallAgeingStats> {
        val durationKey = listOf("Not Due", "1-30", "31-60", "61-90", "91-180", "181-365", ">365")
        return durationKey.map {
            OverallAgeingStats(
                ageingDuration = it,
                amount = BigDecimal(0),
                dashboardCurrency = "INR"
            )
        }
    }

    fun getOutstandingByAgeResponse(): LinkedHashMap<String, OverallAgeingStatsResponse> {
        val durationKey = listOf("Not Due", "1-30", "31-60", "61-90", "91-180", "181-365", ">365")
        val linkedHashMap = linkedMapOf<String, OverallAgeingStatsResponse>()
        durationKey.map {
            linkedHashMap.put(
                it,
                OverallAgeingStatsResponse(
                    ageingDuration = it,
                    amount = BigDecimal(0).setScale(4),
                    dashboardCurrency = "INR"
                )
            )
        }
        return linkedHashMap
    }

    fun getKamWiseResponse(): StatsForKamResponse {
        return StatsForKamResponse(
            totalProformaAmount = BigDecimal(0),
            proformaInvoicesCount = 0,
            customersCountProforma = 0,
            totalDueAmount = BigDecimal(0),
            dueInvoicesCount = 0,
            customersCountDue = 0,
            totalOverdueAmount = BigDecimal(-60.0000).setScale(4),
            overdueInvoicesCount = 1,
            customersCountOverdue = 1,
            totalAmountReceivables = BigDecimal(-60.0000).setScale(4),
            receivablesInvoicesCount = 1,
            customersCountReceivables = 1,
            dueByThirtyDaysAmount = BigDecimal(-60.0000).setScale(4),
            dueBySixtyDaysAmount = BigDecimal(0),
            dueByNinetyDaysAmount = BigDecimal(0),
            dueByNinetyPlusDaysAmount = BigDecimal(0),
            dueByThirtyDaysCount = 1,
            dueBySixtyDaysCount = 0,
            dueByNinetyDaysCount = 0,
            dueByNinetyPlusDaysCount = 0
        )
    }

    fun getCustomerLevelOverallStats(): List<StatsForCustomerResponse> {
        return listOf(
            StatsForCustomerResponse(
                organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
                totalProformaAmount = BigDecimal(0),
                proformaInvoicesCount = 0,
                totalDueAmount = BigDecimal(0),
                dueInvoicesCount = 0,
                totalOverdueAmount = BigDecimal(-60.0000).setScale(4),
                overdueInvoicesCount = 1,
                totalAmountReceivables = BigDecimal(-60.0000).setScale(4),
                receivablesInvoicesCount = 1,
                onAccountPayment = 0.toBigDecimal(),
                dueByThirtyDaysAmount = BigDecimal(-60.0000).setScale(4),
                dueBySixtyDaysAmount = 0.toBigDecimal(),
                dueByNinetyDaysAmount = 0.toBigDecimal(),
                dueByNinetyPlusDaysAmount = 0.toBigDecimal(),
                dueByThirtyDaysCount = 1,
                dueBySixtyDaysCount = 0,
                dueByNinetyDaysCount = 0,
                dueByNinetyPlusDaysCount = 0
            )
        )
    }

    fun getOverallStatsForTradeParty(): List<OverallStatsForTradeParty> {
        return listOf(
            OverallStatsForTradeParty(
                organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
                totalOverdueAmount = BigDecimal(-60.0000).setScale(4),
                totalOutstandingAmount = BigDecimal(-60.0000).setScale(4),
                dueByThirtyDaysAmount = BigDecimal(-60.0000).setScale(4),
                dueBySixtyDaysAmount = BigDecimal(0),
                dueByNinetyDaysAmount = BigDecimal(0),
                dueByNinetyPlusDaysAmount = BigDecimal(0), dueByThirtyDaysCount = 1,
                dueBySixtyDaysCount = 0, dueByNinetyDaysCount = 0, dueByNinetyPlusDaysCount = 0
            )
        )
    }

    fun getInvoiceListForTradeParties(): List<InvoiceListResponse> {
        return listOf(
            InvoiceListResponse(
                organizationId = "9f03db0c-88cc-450f-bbb1-38fa31861911",
                documentNumber = "SINV123455", documentType = "FINAL", serviceType = "FCL_FREIGHT",
                invoiceAmount = BigDecimal(100.0000).setScale(4),
                outstandingAmount = BigDecimal(-60.0000).setScale(4)
            )
        )
    }

    fun getSalesFunnelResponse(): SalesFunnelResponse {
        return SalesFunnelResponse(
            draftInvoicesCount = 0,
            financeAcceptedInvoiceCount = 0,
            irnGeneratedInvoicesCount = 0,
            settledInvoicesCount = 0,
            draftToFinanceAcceptedPercentage = 0,
            financeToIrnPercentage = 0,
            settledPercentage = 0
        )
    }
    fun getInvoiceTatStats(): InvoiceTatStatsResponse {
        return InvoiceTatStatsResponse(
            draftInvoicesCount = 0,
            financeAcceptedInvoiceCount = 0,
            irnGeneratedInvoicesCount = 0,
            settledInvoicesCount = 0,
            tatHoursFromDraftToFinanceAccepted = 0,
            tatHoursFromFinanceAcceptedToIrnGenerated = 0,
            tatHoursFromIrnGeneratedToSettled = 0,
            financeAcceptedInvoiceEventCount = 0,
            irnGeneratedInvoiceEventCount = 0,
            settledInvoiceEventCount = 0
        )
    }

    fun getKamWiseOutstanding(): List<KamWiseOutstanding> {
        return listOf(
            KamWiseOutstanding(
                kamOwners = "vivek_garg",
                openInvoiceAmount = 0.toBigDecimal(),
                totalOutstandingAmount = 0.toBigDecimal()
            )
        )
    }
}
