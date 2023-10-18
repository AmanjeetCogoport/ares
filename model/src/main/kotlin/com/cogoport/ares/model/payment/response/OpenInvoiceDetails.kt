package com.cogoport.ares.model.payment.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDate

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
    var dueDate: LocalDate?,
    var transactionDate: LocalDate?,
    var entityCode: String,
    var serviceType: String,
    var status: String,
    var accType: String,
)
