package com.cogoport.ares.model.payment

enum class AccountType(val dbValue: String) {
    EXP("EXP"),
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
    WOFF("WOFF"),
    ROFF("ROFF"),
    EXCH("EXCH"),
    JVNOS("JVNOS"),
    OUTST("OUTST"),
    PAY("PAY"),
    SREIMB("SREIMB"),
    PREIMB("PREIMB"),
    ICJV("ICJV");

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
