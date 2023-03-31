package com.cogoport.ares.model.payment.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.sql.Timestamp

data class PayableStatsOpenSearchResponse(
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("date")
    var date: Timestamp?,
    @JsonProperty("entity")
    var entity: Int?,
    @JsonProperty("openInvoiceAmount")
    var openInvoiceAmount: BigDecimal?,
    @JsonProperty("onAccountAmount")
    var onAccountAmount: BigDecimal?,
    @JsonProperty("creditNoteAmount")
    var creditNoteAmount: BigDecimal?
)
