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

enum class InvoiceStatus(val value: String) {
    PAID("Paid"), UNPAID("Unpaid"), PARTIAL_PAID("Partially Paid"), KNOCKED_OFF("Knocked Off")
}

enum class InvoiceType(val value: String) {
    SINV("Sales Invoice"), SCN("Sales Credit Note"), SDN("Sales Debit Note"), REC("Sales Payment"), PINV("Purchase Invoice"), PCN("Purchase Credit Note"), PDN("Purchase Debit Note"), PAY("Purchase Payment")
}
