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
    var currency: String,
    @JsonProperty("entity_code")
    var entityCode: Long,
    var amount: BigDecimal? = BigDecimal.ZERO
)

class SagePostInvoiceDetails(
    @JsonProperty("sage_payment_num")
    val sagePaymentNum: String?,
    @JsonProperty("platform_payment_num")
    var platformPaymentNum: String?,
    @JsonProperty("bpr_number")
    var bprNumber: String?,
    @JsonProperty("gl_code")
    var glCode: Long?,
    var currency: String,
    @JsonProperty("entity_code")
    var entityCode: Long,
    var amount: BigDecimal? = BigDecimal.ZERO
)

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
