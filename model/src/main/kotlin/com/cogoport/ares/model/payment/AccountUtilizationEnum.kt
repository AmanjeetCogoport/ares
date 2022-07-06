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

enum class InvoiceStatus {
    PAID, UNPAID, PARTIAL_PAID, KNOCKED_OFF
}

enum class InvoiceType {
    SALES_INVOICES, SALES_CREDIT_NOTE, SALES_DEBIT_NOTE, SALES_PAYMENT, PURCHASE_INVOICES, PURCHASE_CREDIT_NOTE, PURCHASE_DEBIT_NOTE, PURCHASE_PAYMENT
}
