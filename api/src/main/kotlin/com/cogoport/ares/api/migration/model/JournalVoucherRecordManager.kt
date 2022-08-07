package com.cogoport.ares.api.migration.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

class JournalVoucherRecordManager {
    @JsonProperty("recordsets")
    val recordSets: ArrayList<ArrayList<JournalVoucherRecord>>? = null

    @JsonProperty("recordset")
    val recordSet: ArrayList<JournalVoucherRecord>? = null

    @JsonProperty("rowsAffected")
    val recordAffected: ArrayList<Int>? = null

    @JsonIgnore
    val output: Any? = null
}
