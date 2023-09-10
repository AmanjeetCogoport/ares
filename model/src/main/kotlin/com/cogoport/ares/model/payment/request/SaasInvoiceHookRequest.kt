package com.cogoport.ares.model.payment.request

import java.math.BigDecimal
import java.util.Date
import java.util.UUID
import javax.validation.constraints.NotNull

data class SaasInvoiceHookRequest(
    @field: NotNull(message = "Proforma Number cannot be null")
    var proformaId: Long? = null,
    var jobId: Long? = null,
    var utrDetails: List<UTRDetails>? = null,
    var performedBy: UUID? = null,
    @field: NotNull(message = "Performed By cannot be null")
    var performedByUserType: String? = null,
    @field: NotNull(message = "Bank Account Number cannot be null")
    var bankAccountNumber: String? = null,
    @field: NotNull(message = "Bank Name cannot be null")
    var bankName: String? = null,
    var currency: String? = null,
    var entityCode: Int? = null,
)
data class UTRDetails(
    var id: String? = null,
    var utrNumber: String? = null,
    var paidAmount: BigDecimal? = BigDecimal(0),
    var documentUrl: String? = null,
    var transactionDate: Date? = null,
    var status: String?,
    var remark: String? = null
)
