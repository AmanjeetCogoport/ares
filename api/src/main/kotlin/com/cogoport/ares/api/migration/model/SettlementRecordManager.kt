package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class SettlementRecordManager {

    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<SettlementRecord>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<SettlementRecord>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
