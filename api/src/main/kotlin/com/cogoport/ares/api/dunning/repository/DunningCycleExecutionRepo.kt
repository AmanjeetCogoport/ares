package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.model.dunning.response.DunningCycleExecutionResponse
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
            SELECT
                dc.name as name,
                dc.is_active as is_dunning_cycle_active,
                dc.cycle_type as dunning_cycle_type,
                dce.id as id,
                dce.dunning_cycle_id as dunning_cycle_id,
                dce.status as status,
                dce.filters as filters,
                dce.schedule_rule as schedule_rule,
                dce.frequency as frequency,
                dce.scheduled_at as scheduled_at,
                dce.trigger_type as trigger_type,
                dce.deleted_at as deleted_at,
                dce.created_by as created_by,
                dce.updated_by as updated_by,
                dce.created_at as created_at,
                dce.updated_at as updated_at
            FROM
                dunning_cycle_executions dce
            JOIN
                dunning_cycles dc on
                dc.id = dce.dunning_cycle_id
            WHERE
                dce.deleted_at IS NUll
                AND (COALESCE(:status) IS NULL OR dce.status::VARCHAR IN (:status))
                AND (COALESCE(:dunningCycleType) IS NULL OR dc.cycle_type::VARCHAR IN (:dunningCycleType))
                AND (:query IS NULL OR dc.name ILIKE :query)
                 
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
            LIMIT :pageSize
                
        """
    )
    suspend fun listDunningCycleExecution(
        query: String?,
        status: MutableList<String>,
        dunningCycleType: MutableList<String>,
        serviceType: String?,
        sortBy: String? = "created_at",
        sortType: String? = "DESC",
        pageIndex: Int? = 1,
        pageSize: Int? = 10
    ): List<DunningCycleExecutionResponse>

    @NewSpan
    @Query(
        """
            SELECT
                COALESCE(COUNT(dce.id), 0)
            FROM
                dunning_cycle_executions dce
            JOIN
                dunning_cycles dc on
                dc.id = dce.dunning_cycle_id
            WHERE
                dce.deleted_at IS NUll
                AND (:query IS NULL OR dc.name ILIKE :query)
        """
    )
    suspend fun totalCountDunningCycleExecution(
        query: String?,
        status: String?,
        dunningCycleType: String?,
        serviceType: String?
    ): Long

    @NewSpan
    @Query(
        """
            UPDATE dunning_cycle_executions
            SET 
                deleted_at = NOW(),
                status = 'CANCELLED',
                updated_at = NOW(),
                updated_by = :updatedBy
             WHERE id = :id
        """
    )
    suspend fun deleteCycleExecution(
        id: Long,
        updatedBy: UUID
    )

    @Query(
        """
            UPDATE dunning_cycle_executions
            SET 
                status = 'CANCELLED',
                updated_at = NOW(),
                updated_by = :updatedBy
             WHERE id = :id
        """
    )
    suspend fun cancelCycleExecution(
        id: Long,
        updatedBy: UUID
    )

    @Query(
        """
            UPDATE dunning_cycle_executions SET status = :status WHERE id = :id
        """
    )
    suspend fun updateStatus(id: Long, status: String)
}
