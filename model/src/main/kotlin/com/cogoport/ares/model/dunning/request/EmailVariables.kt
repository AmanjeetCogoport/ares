package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.awt.Stroke

@Introspected
@JsonInclude(JsonInclude.Include.ALWAYS)
data class EmailVariables(
        @JsonProperty("bankName")
        var bankName: String? = null,
        @JsonProperty("invoiceUrl")
        var invoiceUrl: String? = null,
        @JsonProperty("accountNumber")
        var accountNumber: String? = null,
        @JsonProperty("accountName")
        var accountName: String? = null,
        @JsonProperty("routingNumber")
        var routingNumber: String? = null,
        @JsonProperty("creditControllerName")
        var creditControllerName: String? = null,
        @JsonProperty("creditControllerEmail")
        var creditControllerEmail: String? = null,
        @JsonProperty("creditControllerMobileCode")
        var creditControllerMobileCode: String? = null,
        @JsonProperty("creditControllerMobileNumber")
        var creditControllerMobileNumber: String? = null,

)
