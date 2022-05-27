package com.cogoport.ares.api.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.IsoFields
import java.util.Calendar

object AresConstants {
    val CURR_QUARTER = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
    val CURR_YEAR = Calendar.getInstance().get(Calendar.YEAR)
    val CURR_DATE: LocalDateTime = LocalDateTime.now()

    const val SALES_DASHBOARD_INDEX = "index_ares_sales_dashboard"
    const val SALES_OUTSTANDING_INDEX = "index_ares_sales_outstanding"
    const val OPEN_SEARCH_DOCUMENT_KEY = "id"
    const val KEY_DELIMITER = "_"

    const val ROLE_ZONE_HEAD = "ZoneHead"
    const val ROLE_SUPER_ADMIN = "SuperAdmin"

    const val SALES_TREND_PREFIX = "SALES_TREND_"
    const val MONTHLY_TREND_PREFIX = "MONTHLY_OUTSTANDING_"
    const val QUARTERLY_TREND_PREFIX = "QUARTERLY_OUTSTANDING_"
    const val COLLECTIONS_TREND_PREFIX = "COLLECTION_TREND_"
    const val OVERALL_STATS_PREFIX = "OVERALL_STATS_"
    const val DAILY_SALES_OUTSTANDING_PREFIX = "DAILY_SALES_"
    const val DAILY_PAYABLES_OUTSTANDING_PREFIX = "DAILY_PAYABLES_"

    const val ZONE = "zone"
    const val ROLE = "role"
    const val QUARTER = "quarter"
    const val YEAR = "year"
    const val DATE = "date"
}
