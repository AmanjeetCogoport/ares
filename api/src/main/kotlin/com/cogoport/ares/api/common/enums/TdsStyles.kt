package com.cogoport.ares.api.common.enums

enum class TdsStyles(val style: String) {
    GROSS("gross"),
    NET("net"),
    EXEMPT("exempt"),
    GROSS_TAXABLE("gross_taxable"),
    GROSS_NON_TAXABLE("gross_non_taxable"),
    NET_TAXABLE("net_taxable"),
    RANDOM("random")
}
