package com.cogoport.ares.api.common

import com.cogoport.ares.model.payment.ServiceType
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
    const val ACCOUNT_UTILIZATION_INDEX = "index_account_utilization"
    val COGO_ENTITIES = listOf(101, 201, 301, 401, 501)
    const val SUPPLIERS_OUTSTANDING_OVERALL_INDEX = "supplier_outstanding_overall"
    const val CUSTOMERS_OUTSTANDING_OVERALL_INDEX = "customer_outstanding_overall"
    const val PAYABLES_STATS_INDEX = "payables_stats"
    const val KEY_DELIMITER = "_"

    const val ROLE_ZONE_HEAD = "b2af88f9-84e4-44fd-92f8-12f74c55e5ae"
    const val ROLE_SUPER_ADMIN = "SuperAdmin"

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
    const val COGO_ENTITY_ID = "cogoEntityId"

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
    const val SUSPENSE_ACCOUNT = "suspense_account"
    const val ACCOUNT_UTILIZATIONS = "account_utilizations"
    const val SETTLEMENT = "settlement"
    const val INCIDENT_MAPPINGS = "incident_mappings"
    const val JOURNAL_VOUCHERS = "journal_vouchers"
    const val PARENT_JOURNAL_VOUCHERS = "parent_journal_vouchers"

    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
    const val VOID = "VOID"
    const val ALL = "All"

    // Settlement Constants
    const val PAYMENT = "PAYMENT"
    const val INVOICE = "INVOICE"
    const val CREDIT_NOTE = "CREDIT_NOTE"
    const val JV = "JV"
    const val TDS = "TDS"
    const val IND = "IND"

    const val UNIFIED = "dms"

    const val LIMIT = 10000

    var WHITE_LISTED_PATHS = mutableListOf(
        "get_payments_health_check",
        "get_payments_service_discovery_reachability",
        "get_payments_service_discovery_plutus_reachability",
        "get_payments_service_discovery_hades_reachability",
        "get_payments_service_discovery_kuber_reachability"
    )

    val ENTITY_ID = mapOf(
        101 to "6fd98605-9d5d-479d-9fac-cf905d292b88",
        201 to "c7e1390d-ec41-477f-964b-55423ee84700",
        301 to "ee09645b-5f34-4d2e-8ec7-6ac83a7946e1",
        401 to "04bd1037-c110-4aad-8ecc-fc43e9d4069d",
        501 to "b67d40b1-616c-4471-b77b-de52b4c9f2ff"
    )
    val TAGGED_ENTITY_ID_MAPPINGS = mapOf(
        "6fd98605-9d5d-479d-9fac-cf905d292b88" to 101,
        "c7e1390d-ec41-477f-964b-55423ee84700" to 201,
        "ee09645b-5f34-4d2e-8ec7-6ac83a7946e1" to 301,
        "04bd1037-c110-4aad-8ecc-fc43e9d4069d" to 401,
        "b67d40b1-616c-4471-b77b-de52b4c9f2ff" to 501,
    )

    val LEDGER_CURRENCY = mapOf(
        101 to "INR",
        201 to "EUR",
        301 to "INR",
        401 to "SGD",
        501 to "VND",
    )

    val OCEAN_SERVICES = listOf(
        ServiceType.FCL_FREIGHT,
        ServiceType.LCL_FREIGHT,
        ServiceType.FCL_CUSTOMS,
        ServiceType.LCL_CUSTOMS,
        ServiceType.FCL_CUSTOMS_FREIGHT,
        ServiceType.LCL_CUSTOMS_FREIGHT,
        ServiceType.FCL_CFS,
        ServiceType.FCL_FREIGHT_LOCAL
    )
    val AIR_SERVICES = listOf(
        ServiceType.AIR_CUSTOMS,
        ServiceType.AIR_FREIGHT,
        ServiceType.AIR_CUSTOMS_FREIGHT,
        ServiceType.AIR_FREIGHT_LOCAL,
        ServiceType.DOMESTIC_AIR_FREIGHT
    )
    val SURFACE_SERVICES = listOf(
        ServiceType.FTL_FREIGHT,
        ServiceType.LTL_FREIGHT,
        ServiceType.HAULAGE_FREIGHT,
        ServiceType.TRAILER_FREIGHT,
        ServiceType.RAIL_DOMESTIC_FREIGHT,
        ServiceType.TRUCKING
    )
}
