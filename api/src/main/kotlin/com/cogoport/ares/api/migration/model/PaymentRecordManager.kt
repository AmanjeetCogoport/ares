package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class PaymentRecordManager {

    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<PaymentRecord>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<PaymentRecord>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
