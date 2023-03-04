package com.cogoport.ares.model.payment

enum class CompanyType(val value: String) {
    MIDSIZE("midsize"), ENTERPRISE("enterprise"), LONGTAIL("longtail")
}

enum class DocumentType(val value: String) {
    SALES_INVOICE("SALES_INVOICE"), CREDIT_NOTE("CREDIT_NOTE"), ON_ACCOUNT_PAYMENT("ON_ACCOUNT_PAYMENT"), SHIPMENT_CREATED("SHIPMENT_CREATED")
}
