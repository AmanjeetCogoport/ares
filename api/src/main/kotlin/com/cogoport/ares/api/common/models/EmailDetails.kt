package com.cogoport.ares.api.common.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType

@Introspected
@MappedEntity
data class EmailDetails(
    var organizationId: String? = null,
    var invoicePdfUrl: String ? = null,
    var bankName: String? = null,
    var beneficiaryName: String? = null,
    var ifscCode: String? = null,
    var swiftCode: String? = null,
    var accountNumber: String? = null,
    @JsonProperty("credit_controller_details")
    @MappedProperty(type = DataType.JSON)
    var creditControllerDetails: ArrayList<CreditControllerDetails>? = ArrayList(),
    @JsonProperty("customer_email")
    var customerEmail: ArrayList<String>? = ArrayList()
)
