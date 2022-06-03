package com.cogoport.ares.model.payment

enum class AccountType(dbValue: String) {
    SINV("SINV"), PINV("PINV"), SCN("SCN"), SDN("SDN"), PCN("PCN"), PDN("PDN"), REC("REC"), PAY("PAY");

    open operator fun contains(value: String?): Boolean {
        for (c in AccountType.values()) {
            if (c.equals(value)) {
                return true
            }
        }
        return false
    }
}

enum class PaymentCode {
    PAY, REC, CTDS, VTDS
}
