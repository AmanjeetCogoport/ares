package com.cogoport.ares.model.settlement.enums

enum class JVStatus(val value: String) {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    DELETED("DELETED"),
    UTILIZED("UTILIZED"),
    POSTED("POSTED"),
    POSTING_FAILED("POSTING_FAILED");

    open operator fun contains(value: String?): Boolean {
        for (c in JVStatus.values()) {
            if (c.equals(value)) {
                return true
            }
        }
        return false
    }
}
