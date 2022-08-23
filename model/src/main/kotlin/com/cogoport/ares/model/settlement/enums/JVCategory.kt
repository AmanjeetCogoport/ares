package com.cogoport.ares.model.settlement.enums

enum class JVCategory(val value: String) {
    EXCH("EXCH"),
    JVNOS("JVNOS"),
    WOFF("WOFF"),
    ROFF("ROFF"),
    OUTST("OUTST");

    open operator fun contains(value: String?): Boolean {
        for (c in JVCategory.values()) {
            if (c.equals(value)) {
                return true
            }
        }
        return false
    }
}
