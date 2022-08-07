package com.cogoport.ares.model.payment

enum class AccountType(val dbValue: String) {
    SINV("SINV"),
    PINV("PINV"),
    SCN("SCN"),
    SDN("SDN"),
    PCN("PCN"),
    PDN("PDN"),
    REC("REC"),
    OPDIV("OPDIV"),
    MISC("MISC"),
    BANK("BANK"),
    CONTR("CONTR"),
    INTER("INTER"),
    MTC("MTC"),
    MTCCV("MTCCV"),
    PAY("PAY");

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
    PAY, REC, CTDS, VTDS, CPRE, APRE
}

enum class Operator {
    DIVIDE, MULTIPLY, SUBTRACT, ADD
}
