package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementKnockoffRequest(
    val invoiceNumber: String,
    val accCode: Int,
    val cogoAccountNo: String,
    val payMode: String, // todo payment mode enum

    var transactionId: String,
    val transactionDate: Timestamp,
    val amount: BigDecimal,
    val currency: String,
//    val exchange_rate: Float,
    val fee: BigDecimal,
    val tax: BigDecimal,

//    var accMode: AccMode = AccMode.AR,

    val email: String, // narration or audits
    val performedBy: UUID?,
    val performedByUserType: String?
)
