package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
data class PlatformOrganizationResponse(

    @JsonProperty("id")
    val organizationId:UUID,

    @JsonProperty("organization_name")
    val organizationName: String?,

    @JsonProperty("organization_serial_id")
    val organizationSerialId: Long?,

    @JsonProperty("zone")
    val zone: String?,

    @JsonProperty("category_types")
    val categoryTypes: String?

)
