package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementKnockoffRequest(
    val invoiceNumber: Long,
    val accCode: Int,
    val cogoAccountNo: String,

    val entityCode: Int,
    val bankId: String,
    val bankName: String,
    val payMode: String, // todo payment mode enum

    var transactionId: String,
    val transactionDate: String,
    val amount: BigDecimal,
    val currency: String,
//    val exchange_rate: Float,
    val fee: BigDecimal,
    val tax: BigDecimal,

    val email: String, // narration or audits
)
