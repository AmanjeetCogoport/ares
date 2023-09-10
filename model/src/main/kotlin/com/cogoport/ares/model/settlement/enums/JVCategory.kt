package com.cogoport.ares.model.settlement.enums

enum class JVCategory(val value: String) {
    EXCH("Exchange"),
    JVNOS("Nostro"),
    WOFF("Write Off"),
    ROFF("Round Off"),
    OUTST("Outstanding"),
    IEJV("Inter Entity"),
    ICJV("Intercompany");

    open operator fun contains(value: String?): Boolean {
        for (c in JVCategory.values()) {
            if (c.equals(value)) {
                return true
            }
        }
        return false
    }
}
