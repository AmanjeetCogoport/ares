package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccountType

enum class SettlementType(val dbValue: String) {
    SINV("SINV"),
    PINV("PINV"),
    SCN("SCN"),
    SDN("SDN"),
    PCN("PCN"),
    PDN("PDN"),
    REC("REC"),
    PAY("PAY"),
    SECH("SECH"),
    PECH("PECH"),
    VTDS("VTDS"),
    CTDS("CTDS");

    open operator fun contains(value: String?): Boolean {
        for (c in SettlementType.values()) {
            if (c.equals(value)) {
                return true
            }
        }
        return false
    }
}
