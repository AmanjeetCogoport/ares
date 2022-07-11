package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.cogoport.ares.model.common.KnockOffStatus
import com.cogoport.ares.model.payment.PaymentCode
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.http.annotation.QueryValue
import java.math.BigDecimal
import java.util.*

@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SettlementKnockoffRequest (
    val invoiceNumber: Long,
    var organizationId: String,
    var organizationName: String,
    val accCode: Int,
    val cogoAccountNo: String,

    val entityCode: Int,
    val bankId: String,
    val bankName: String,
    val payMode: String, // todo payment mode enum

    var transactionId: String,
    val transactionDate: Date,
    val amount: BigDecimal,
    val currency: String,
//    val exchange_rate: Float,
    val fee: BigDecimal,
    val tax: BigDecimal,

    val email: String,  // narration or audits

    val bankTransactionId: String

//    TODO payment_num Auto generate
//    TODO
)