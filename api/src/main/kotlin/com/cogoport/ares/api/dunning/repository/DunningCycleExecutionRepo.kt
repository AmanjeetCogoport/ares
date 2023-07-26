package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface DunningCycleExecutionRepo : CoroutineCrudRepository<DunningCycleExecution, Long> {

    @NewSpan
    @Query(
        """
            UPDATE dunning_cycle_executions SET status = :status, updated_at = NOW() WHERE id = :id
        """
    )
    suspend fun updateStatus(id: Long, status: String)

    @NewSpan
    @Query(
        """
            UPDATE dunning_cycle_executions SET service_id = :serviceId, updated_at = NOW() where id = :id
        """
    )
    suspend fun updateServiceId(id: Long, serviceId: String)

    @NewSpan
    @Query(
        """
             UPDATE dunning_cycle_executions SET status = 'CANCELLED',
             updated_at = now(),
             updated_by = :updatedBy
             WHERE dunning_cycle_id = :dunningId
             AND status = 'SCHEDULED'
        """
    )
    suspend fun cancelExecutions(dunningId: Long, updatedBy: UUID)

    @NewSpan
    @Query(
        """
        SELECT EXISTS(SELECT * FROM dunning_cycle_executions where dunning_cycle_id = :dunningId AND status = 'SCHEDULED')
    """
    )
    suspend fun isScheduledExecutionExist(dunningId: Long): Boolean
}
