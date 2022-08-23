package com.cogoport.ares.api.settlement.repository

import com.cogoport.ares.api.common.enums.IncidentStatus
import com.cogoport.ares.api.settlement.entity.IncidentMappings
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface IncidentMappingsRepository : CoroutineCrudRepository<IncidentMappings, Long> {
    @Query(
        """
            UPDATE incident_mappings
            SET incident_status = :status
            WHERE incident_type = 'SETTLEMENT_APPROVAL'
            AND id = :incidentMappingId
        """
    )
    suspend fun updateStatus(incidentMappingId: Long, status: IncidentStatus)
}
