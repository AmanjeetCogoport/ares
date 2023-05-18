package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties
@MappedEntity
data class SagePaymentNumMigrationResponse(
    @JsonProperty("sage_ref_num") var sageRefNum: String? = null,
    @JsonProperty("sage_payment_num") var sagePaymentNum: String? = null
)

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties
class PaymentNumInfo {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<SagePaymentNumMigrationResponse>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<SagePaymentNumMigrationResponse>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null

    @JsonProperty
    val error: Any? = null
}
