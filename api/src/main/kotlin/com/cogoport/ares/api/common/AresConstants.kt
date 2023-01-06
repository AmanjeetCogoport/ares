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

    // Open search indexes
    const val SALES_DASHBOARD_INDEX = "index_ares_sales_dashboard"
    const val SALES_OUTSTANDING_INDEX = "index_ares_sales_outstanding"
    const val INVOICE_OUTSTANDING_INDEX = "index_ares_invoice_outstanding"
    const val ON_ACCOUNT_PAYMENT_INDEX = "index_ares_on_account_payment"
    const val ACCOUNT_UTILIZATION_INDEX = "index_account_utilization"
    const val SUPPLIERS_OUTSTANDING_OVERALL_INDEX = "supplier_outstanding_overall"
    const val SUPPLIERS_OUTSTANDING_101_INDEX = "supplier_outstanding_101"
    const val SUPPLIERS_OUTSTANDING_201_INDEX = "supplier_outstanding_201"
    const val SUPPLIERS_OUTSTANDING_301_INDEX = "supplier_outstanding_301"
    const val SUPPLIERS_OUTSTANDING_401_INDEX = "supplier_outstanding_401"
    const val ENTITY_CODE_101 = 101
    const val ENTITY_CODE_201 = 201
    const val ENTITY_CODE_301 = 301
    const val ENTITY_CODE_401 = 401
    const val KEY_DELIMITER = "_"

    const val ROLE_ZONE_HEAD = "b2af88f9-84e4-44fd-92f8-12f74c55e5ae"
    const val ROLE_SUPER_ADMIN = "SuperAdmin"

    const val MONTHLY_TREND_PREFIX = "MONTHLY_OUTSTANDING_"
    const val QUARTERLY_TREND_PREFIX = "QUARTERLY_OUTSTANDING_"
    const val COLLECTIONS_TREND_PREFIX = "COLLECTION_TREND_"
    const val OVERALL_STATS_PREFIX = "OVERALL_STATS_"
    const val DAILY_SALES_OUTSTANDING_PREFIX = "DAILY_SALES_"
    const val DAILY_PAYABLES_OUTSTANDING_PREFIX = "DAILY_PAYABLES_"

    const val ZONE = "zone"
    const val QUARTER = "quarter"
    const val YEAR = "year"
    const val DATE = "date"
    const val ORG_ID = "orgId"
    const val ORG_NAME = "orgName"
    const val PAGE = "page"
    const val PAGE_LIMIT = "pageLimit"
    const val ACC_MODE = "AccMode"
    const val SERVICE_TYPE = "serviceType"
    const val INVOICE_CURRENCY = "invoiceCurrency"
    const val DASHBOARD_CURRENCY = "dashboardCurrency"

    const val YEAR_DATE_FORMAT = "yyyy-MM-dd"

    const val ACCMODE = "accMode"
    const val MODE = "AR"
    const val ORGANIZATION_ID = "organizationId"
    const val TRANSACTION_DATE = "transactionDate"

    const val DEFAULT_TDS_RATE = 2
    const val DECIMAL_NUMBER_UPTO = 12
    const val ROUND_DECIMAL_TO = 4
    const val NO_DEDUCTION_RATE = 0

    const val PAYING_PARTY = "self,paying_party"

    const val PAYMENTS = "payments"
    const val ACCOUNT_UTILIZATIONS = "account_utilizations"
    const val SETTLEMENT = "settlement"
    const val INCIDENT_MAPPINGS = "incident_mappings"
    const val JOURNAL_VOUCHERS = "journal_vouchers"

    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"

    const val ALL = "All"

    // Settlement Constants
    const val PAYMENT = "PAYMENT"
    const val INVOICE = "INVOICE"
    const val CREDIT_NOTE = "CREDIT_NOTE"
    const val JV = "JV"

    const val LIMIT = 10000

    const val AGEING_NOT_DUE = "not_due"
    const val AGEING_DUE_TODAY = "today"
    const val AGEING_1_30 = "1_30"
    const val AGEING_31_60 = "31_60"
    const val AGEING_61_90 = "61_90"
    const val AGEING_91_180 = "91_180"
    const val AGEING_181_365 = "181_365"
    const val AGEING_365_PLUS = "365"
}
