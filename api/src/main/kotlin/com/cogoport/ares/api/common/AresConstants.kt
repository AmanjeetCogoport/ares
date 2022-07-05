package com.cogoport.ares.api.common

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.IsoFields

object AresConstants {
    val CURR_QUARTER = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
    val CURR_YEAR = LocalDate.now().year
    val CURR_DATE: Timestamp = Timestamp.from(Instant.now())
    val CURR_MONTH = LocalDate.now().monthValue

    const val SALES_DASHBOARD_INDEX = "index_ares_sales_dashboard"
    const val SALES_OUTSTANDING_INDEX = "index_ares_sales_outstanding"
    const val INVOICE_OUTSTANDING_INDEX = "index_ares_invoice_outstanding"
    const val ON_ACCOUNT_PAYMENT_INDEX = "index_ares_on_account_payment"
    const val ACCOUNT_UTILIZATION_INDEX = "index_account_utilization"
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
    const val ACC_MODE = "AccMode"

    const val YEAR_DATE_FORMAT = "yyyy-MM-dd"

    const val ACCMODE = "accMode"
    const val MODE = "AR"
    const val ORGANIZATION_ID = "organizationId"
    const val TRANSACTION_DATE = "transactionDate"

    const val PAYMENTS = "payments"
    const val ACCOUNT_UTILIZATIONS = "account_utilizations"

    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
}
