package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class NewPeriodRecordManager {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<NewPeriodRecord>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<NewPeriodRecord>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
