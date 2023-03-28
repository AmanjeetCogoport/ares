package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class JVRecordsWithoutBprManager {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<JVLineItemNoBPR>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<JVLineItemNoBPR>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
