package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class SagePostPaymentDetails(
    @JsonProperty("sage_payment_num")
    val sagePaymentNum: String?,
    @JsonProperty("platform_payment_num")
    var platformPaymentNum: String?,
    @JsonProperty("bpr_number")
    var bprNumber: String?,
    @JsonProperty("gl_code")
    var glCode: Long?,
    @JsonProperty("currency")
    var currency: String,
    @JsonProperty("entity_code")
    var entityCode: Long,
    @JsonProperty("amount")
    var amount: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("sage_status") var sageStatus: String
)
@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class SagePostInvoiceDetails(
    @JsonProperty("invoice_number")
    val invoiceNumber: String?,
    @JsonProperty("sage_status")
    var sageStatus: String?,
    @JsonProperty("bpr_number")
    var bprNumber: String?,
    @JsonProperty("job_number")
    var jobNumber: String?,
    @JsonProperty("currency")
    var currency: String?,
    @JsonProperty("exchange_rate")
    var exchange_rate: BigDecimal? = BigDecimal.ONE,
    @JsonProperty("name")
    var name: String?,
    @JsonProperty("tax_amount")
    var taxAmount: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("grand_total")
    var grandTotal: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("ledger_total")
    var ledgerTotal: BigDecimal? = BigDecimal.ZERO
)
@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PostPaymentInfo {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<SagePostPaymentDetails>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<SagePostPaymentDetails>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PostInvoiceInfo {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<SagePostInvoiceDetails>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<SagePostInvoiceDetails>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
