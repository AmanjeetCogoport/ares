package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.util.Date

@Introspected
@MappedEntity
data class OpenInvoiceDetails(
    var documentNo: Long,
    var organizationName: String,
    var documentValue: String,
    var currency: String,
    var amountCurr: String,
    var amountLoc: String,
    var payCurr: String,
    var payLoc: String,
    var dueDate: Date?,
    var transactionDate: Date?,
    var entityCode: String,
    var serviceType: String,
    var status: String,
    var accType: String,
)
