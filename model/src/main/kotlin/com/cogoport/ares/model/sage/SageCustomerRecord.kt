package com.cogoport.ares.model.sage

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected

@Introspected
class SageCustomerRecord {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<SageCustomer>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<SageCustomer>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
