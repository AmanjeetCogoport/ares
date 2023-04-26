package com.cogoport.ares.api.common.enums

enum class SequenceSuffix(val prefix: String) {
    PAYMENT("PAY"),
    RECEIVED("REC"),
    TDS("TDS"),
    JV("JV"),
    SETTLEMENT("SETL"),
    CTDSP("CTDSP"),
    CTDS("CTDS"),
    VTDS("VTDS")
}
