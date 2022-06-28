package com.cogoport.ares.model.payment

enum class AccMode {
    AR, AP;
}

enum class ServiceType {
    FCL_FREIGHT,
    LCL_FREIGHT,
    AIR_FREIGHT,
    FTL_FREIGHT,
    LTL_FREIGHT,
    HAULAGE_FREIGHT,
    FCL_CUSTOMS,
    AIR_CUSTOMS,
    LCL_CUSTOMS,
    NA,
    TRAILER_FREIGHT,
    STORE_ORDER,
    ADDITIONAL_CHARGE,
    FCL_CFS,
    ORIGIN_SERVICES,
    DESTINATION_SERVICES
}

enum class ZoneCode {
    NORTH, SOUTH, EAST, WEST
}

enum class AllCurrencyTypes {
    GBP, EUR, USD, INR;
}

enum class PaymentInvoiceMappingType {
    TDS, INVOICE, BILL
}
