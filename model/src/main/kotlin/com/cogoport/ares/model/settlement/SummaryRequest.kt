package com.cogoport.ares.model.settlement

import com.cogoport.ares.model.common.AresModelConstants
import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.QueryValue
import java.sql.Timestamp
import java.util.UUID
import javax.validation.constraints.NotNull

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class SummaryRequest(
    @field:NotNull(message = "entityCode cannot be null")
    val entityCode: Int?,
    @QueryValue(AresModelConstants.IMPORTER_EXPORTER_ID) val importerExporterId: UUID? = null,
    @QueryValue(AresModelConstants.SERVICE_PROVIDER_ID) val serviceProviderId: UUID? = null,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null
)
