package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class JVRecordsForSchedulerManager {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<JVRecordsScheduler>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<JVRecordsScheduler>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
