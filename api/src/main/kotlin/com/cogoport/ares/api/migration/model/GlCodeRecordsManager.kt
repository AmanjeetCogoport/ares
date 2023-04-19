package com.cogoport.ares.api.migration.model

import com.cogoport.ares.model.settlement.GlCodeMaster
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class GlCodeRecordsManager {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<GlCodeMaster>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<GlCodeMaster>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
