package com.cogoport.ares.model.settlement

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.sql.Timestamp
import java.util.UUID

@Introspected
data class GlCodeMaster(
    @JsonProperty("account_code")
    var accountCode: Int,
    @JsonProperty("description")
    var description: String?,
    @JsonProperty("led_account")
    var ledAccount: String,
    @JsonProperty("account_type")
    var accountType: String?,
    @JsonProperty("class_code")
    var classCode: Int,
    @JsonProperty("created_by")
    var createdBy: UUID,
    @JsonProperty("updated_by")
    var updatedBy: UUID,
    @JsonProperty("created_at")
    var createdAt: Timestamp?,
    @JsonProperty("updated_at")
    var updatedAt: Timestamp?
)
