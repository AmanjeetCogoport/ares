package com.cogoport.ares.model.payment

enum class DocumentStatus(dbValue: String) {

    PROFORMA("PROFORMA"), FINAL("FINAL"), CANCELLED("CANCELLED"), DELETED("DELETED")
}
