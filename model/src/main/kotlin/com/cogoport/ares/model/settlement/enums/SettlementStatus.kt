package com.cogoport.ares.model.settlement.enums

enum class SettlementStatus(val value: String) {
    CREATED("CREATED"),
    POSTED("POSTED"),
    POSTING_FAILED("POSTING_FAILED"),
    DELETED("DELETED")
}
