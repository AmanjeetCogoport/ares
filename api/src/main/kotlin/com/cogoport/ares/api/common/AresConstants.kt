package com.cogoport.ares.api.common

import com.fasterxml.jackson.annotation.JsonProperty
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.util.Calendar

object AresConstants {
    val CURR_QUARTER = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
    val CURR_YEAR = Calendar.getInstance().get(Calendar.YEAR)
    val CURR_DATE: Timestamp = Timestamp.from(Instant.now())
    val CURR_MONTH = Calendar.getInstance().get(Calendar.MONTH)

    const val SALES_DASHBOARD_INDEX = "index_ares_sales_dashboard"
    const val SALES_OUTSTANDING_INDEX = "index_ares_sales_outstanding"
    const val INVOICE_OUTSTANDING_INDEX = "index_ares_invoice_outstanding"
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
    const val ORG_ID = "orgId"
    const val ORG_NAME = "orgName"
    const val PAGE = "page"
    const val PAGE_LIMIT = "pageLimit"
}
