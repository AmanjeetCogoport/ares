package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
data class PaymentSummary(
    @JsonProperty("payment_num")
    val paymentNum: String?,
    @JsonProperty("account_util_amt_led")
    val ledAmount: BigDecimal?,
    @JsonProperty("sign_flag")
    val signFlag: Int?,
    @JsonProperty("transaction_date")
    val transactionDate: String?,
)
