package com.cogoport.ares.model.common

enum class AuditObjectType(val value: String) {
    DUNNING_CYCLE("dunning_cycle"),
    DUNNING_CYCLE_EXECUTION("dunning_cycle_execution"),
    ACCOUNT_UTILIZATIONS("account_utilizations"),
    PAYMENTS("payments"),
    SETTLEMENT("settlement"),
    JOURNAL_VOUCHERS("journal_vouchers"),
    PARENT_JOURNAL_VOUCHERS("parent_journal_vouchers"),
    JOBS("jobs"),
    PAYMENT_INVOICE_MAP("payment_invoice_map")
}
