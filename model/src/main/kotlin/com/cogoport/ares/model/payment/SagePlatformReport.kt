package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.util.Date
import kotlin.collections.ArrayList

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties
class PostPaymentInfos {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<SagePaymentDetails>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<SagePaymentDetails>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null

    @JsonProperty
    val error: Any? = null
}

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties
class SagePaymentDetails(
    @JsonProperty("sage_payment_num")
    val sagePaymentNum: String?,
    @JsonProperty("platform_payment_num")
    var platformPaymentNum: String?,
    @JsonProperty("bpr_number")
    var bprNumber: String?,
    @JsonProperty("gl_code")
    var glCode: Long?,
    @JsonProperty("currency")
    var currency: String?,
    @JsonProperty("entity_code")
    var entityCode: Long?,
    @JsonProperty("amount")
    var amount: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("sage_status") var sageStatus: String?,
    @JsonProperty("organization_name") var organizationName: String?,
    @JsonProperty("pan_number") var panNumber: String?,
    @JsonProperty("payment_code") var paymentCode: String?,
    @JsonProperty("acc_mode") var accMode: String?,
    @JsonProperty("transaction_date") var transactionDate: Date,
    @JsonProperty("narration") var narration: String?,
    @JsonProperty("sage_organization_id") var sageOrganizationId: String?
)
