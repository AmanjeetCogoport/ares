package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class InvoiceDetailRecordManager {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<InvoiceDetails>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<InvoiceDetails>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
