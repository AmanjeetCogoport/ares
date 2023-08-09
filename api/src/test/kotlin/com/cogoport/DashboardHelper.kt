package com.cogoport

import com.cogoport.ares.api.payment.entity.DailyOutstanding
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.model.payment.DailySalesOutstanding
import com.cogoport.ares.model.payment.QuarterlyOutstanding
import com.cogoport.ares.model.payment.response.DsoResponse
import com.cogoport.ares.model.payment.response.QsoResponse
import jakarta.inject.Singleton
import java.math.BigDecimal

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
        val monthList = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEPT", "OCT", "NOV", "DEC")
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

    fun getOutstanding(): MutableList<Outstanding> {
        return mutableListOf(
            Outstanding(
                duration = "Q1"
            )
        )
    }

    fun getQuarterlyOutstanding(): QuarterlyOutstanding {
        val quarterList = listOf("JAN - MAR", "APR - JUN", "JUL - SEPT", "OCT - DEC")
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
}
