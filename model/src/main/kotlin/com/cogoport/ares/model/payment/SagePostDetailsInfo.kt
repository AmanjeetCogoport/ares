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
class PlatformPostPaymentDetails(
    @JsonProperty("sage_ref_number")
    val sagePaymentNum: String?,
    @JsonProperty("payment_num_value")
    var platformPaymentNum: String?,
    @JsonProperty("sage_organization_id")
    var bprNumber: String?,
    @JsonProperty("acc_code")
    var glCode: Long?,
    @JsonProperty("currency")
    var currency: String,
    @JsonProperty("entity_code")
    var entityCode: Long,
    @JsonProperty("amount")
    var amount: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("status") var status: String
)

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PaymentDetailsInfo(
    var sagePaymentInfo: List<SagePostPaymentDetails>,
    var platformPaymentInfo: List<PlatformPostPaymentDetails>
)
