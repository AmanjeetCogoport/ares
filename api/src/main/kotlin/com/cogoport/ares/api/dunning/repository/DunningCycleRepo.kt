package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.DunningCycle
import com.cogoport.ares.model.dunning.response.DunningCycleExecutionResponse
import com.cogoport.ares.model.dunning.response.DunningCycleResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface DunningCycleRepo : CoroutineCrudRepository<DunningCycle, Long> {

    @Query(
        """
            UPDATE 
                dunning_cycles 
            SET 
                is_active = :status 
            WHERE 
                id = :id
        """
    )
    suspend fun updateStatus(id: Long, status: Boolean)

    @NewSpan
    @Query(
        """
            SELECT
                id,
                name,
                schedule_rule,
                cycle_type::varchar,
                created_at::timestamp,
                updated_at::timestamp
            FROM
                dunning_cycles
            WHERE
                deleted_at IS NULL AND
                (:query IS NULL OR name ILIKE :query) AND
                (:status IS NULL OR is_active = :status)
           OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
           LIMIT :pageSize
        """
    )
    suspend fun listDunningCycle(
        query: String?,
        status: Boolean?,
        sortBy: String? = "created_at",
        sortType: String? = "DESC",
        pageIndex: Int? = 1,
        pageSize: Int? = 10
    ): List<DunningCycleResponse>

    @NewSpan
    @Query(
        """
            SELECT
                COALESCE(COUNT(*), 0)
            FROM
                dunning_cycles
            WHERE
                (:query IS NULL OR name ILIKE :query) AND
                (:status IS NULL OR is_active = :status) AND
                deleted_at IS NULL
        """
    )
    suspend fun totalCountDunningCycle(
        query: String?,
        status: Boolean?,
    ): Long

    @NewSpan
    @Query(
        """
             UPDATE dunning_cycles
             SET 
                deleted_at = NOW(),
                updated_at = NOW(),
                updated_by = :updatedBy
             WHERE id = :id
        """
    )
    suspend fun deleteCycle(
        id: Long,
        updatedBy: UUID
    )

    @NewSpan
    @Query(
        """
            SELECT severity_level FROM dunning_cycles WHERE id = :id
        """
    )

    suspend fun getSeverityTemplate(id: Long): Int

    @NewSpan
    @Query(
        """
            SELECT
                dc.name AS name,
                dc.is_active AS is_dunning_cycle_active,
                dc.cycle_type AS dunning_cycle_type,
                dce.id AS id,
                dce.dunning_cycle_id AS dunning_cycle_id,
                dce.status AS status,
                dce.filters AS filters,
                dce.schedule_rule AS schedule_rule,
                dce.frequency AS frequency,
                dce.scheduled_at AS scheduled_at,
                dce.trigger_type AS trigger_type,
                dce.deleted_at AS deleted_at,
                dce.created_by AS created_by,
                dce.updated_by AS updated_by,
                dce.created_at AS created_at,
                dce.updated_at AS updated_at
            FROM
                dunning_cycles dc
            LEFT JOIN LATERAL (
                SELECT *
                FROM dunning_cycle_executions dce_inner
                WHERE dce_inner.dunning_cycle_id = dc.id
                ORDER BY dce_inner.updated_at
                LIMIT 1
            ) dce ON true                
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
                count(*)
            FROM
                dunning_cycles dc
            LEFT JOIN LATERAL (
                SELECT *
                FROM dunning_cycle_executions dce_inner
                WHERE dce_inner.dunning_cycle_id = dc.id
                ORDER BY dce_inner.updated_at
                LIMIT 1
            ) dce ON true                
            WHERE
                dce.deleted_at IS NUll
                AND (COALESCE(:status) IS NULL OR dce.status::VARCHAR IN (:status))
                AND (COALESCE(:dunningCycleType) IS NULL OR dc.cycle_type::VARCHAR IN (:dunningCycleType))
                AND (:query IS NULL OR dc.name ILIKE :query)
        """
    )
    suspend fun totalCountDunningCycleExecution(
        query: String?,
        status: MutableList<String>,
        dunningCycleType: MutableList<String>,
        serviceType: String?
    ): Long
}
