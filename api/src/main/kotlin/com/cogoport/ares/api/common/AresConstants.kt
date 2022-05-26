package com.cogoport.ares.api.common

import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.*

object AresConstants {
    val CURR_QUARTER = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
    val CURR_YEAR = Calendar.getInstance().get(Calendar.YEAR)
    const val SALES_DASHBOARD_INDEX = "index_ares_sales_dashboard"
    const val SALES_OUTSTANDING_INDEX = "index_ares_sales_outstanding"
    const val OPEN_SEARCH_DOCUMENT_KEY = "docKey"
    const val OPEN_SEARCH_DOCUMENT_KEY_DELIMITER = "_"

    const val ROLE_ZONE_HEAD = "ZoneHead"
    const val MONTHLY_TREND_PREFIX = "monthly_outstanding_"
    const val QUARTERLY_TREND_PREFIX = "quarterly_outstanding_"
    const val COLLECTIONS_TREND_PREFIX = "collections_trend_"
    const val OVERALL_STATS_PREFIX = "overall_stats_"
    const val ROLE_SUPER_ADMIN = "SuperAdmin"
    const val SALES_TREND_PREFIX = "sales_trend_"
    const val DAILY_SALES_OUTSTANDING_PREFIX = "daily_sales_"
    const val DAILY_PAYABLES_OUTSTANDING_PREFIX = "daily_payables_"

    const val ZONE = "zone"
    const val ROLE = "role"
    const val QUARTER = "quarter"
    const val YEAR = "year"

}
