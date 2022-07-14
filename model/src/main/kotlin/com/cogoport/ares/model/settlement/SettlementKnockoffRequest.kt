package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.payment.AccMode
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.sql.Timestamp

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementKnockoffRequest(
    val invoiceNumber: String,
    val accCode: Int,
    val cogoAccountNo: String,

    val entityCode: Int,
    val bankId: String,
    val bankName: String,
    val payMode: String, // todo payment mode enum

    var transactionId: String,
    val transactionDate: Timestamp,
    val amount: BigDecimal,
    val currency: String,
//    val exchange_rate: Float,
    val fee: BigDecimal,
    val tax: BigDecimal,

    var accMode: AccMode = AccMode.AR,

    val email: String, // narration or audits
)
