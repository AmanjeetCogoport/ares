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
                is_active = :status,
                updated_at = now()
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
                filters,
                frequency,
                cycle_type::varchar,
                trigger_type,
                created_at::timestamp,
                updated_at::timestamp,
                is_active AS is_dunning_cycle_active
            FROM
                dunning_cycles
            WHERE
                deleted_at IS NULL AND
                (:query IS NULL OR name ILIKE :query) AND
                (:dunningCycleType IS NULL OR cycle_type::varchar = :dunningCycleType)
                 ORDER BY
                 CASE WHEN :sortType = 'ASC' AND :sortBy = 'createdAt' THEN created_at END ASC,
                 CASE WHEN :sortType = 'DESC' AND :sortBy = 'createdAt' THEN created_at END DESC,
                 CASE WHEN :sortType = 'ASC' AND :sortBy = 'updatedAt' THEN updated_at END ASC,
                 CASE WHEN :sortType = 'DESC' AND :sortBy = 'updatedAt' THEN updated_at END DESC
           OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
           LIMIT :pageSize
        """
    )
    suspend fun listDunningCycle(
        query: String? = null,
        sortBy: String? = "createdAt",
        sortType: String? = "DESC",
        dunningCycleType: String?,
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
                (:dunningCycleType IS NULL OR cycle_type::varchar = :dunningCycleType) AND
                deleted_at IS NULL
        """
    )
    suspend fun totalCountDunningCycle(
        query: String? = null,
        status: Boolean? = false,
        dunningCycleType: String? = null
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
                id,
                dunning_cycle_id,
                status,
                filters,
                schedule_rule,
                frequency,
                scheduled_at,
                trigger_type,
                created_at AS created_at,
                service_id
            FROM dunning_cycle_executions
            WHERE 
            (:dunningCycleId IS NULL OR dunning_cycle_id = :dunningCycleId)
            AND (:serviceId IS NULL OR service_id::uuid = :serviceId)
            ORDER BY created_at DESC
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
           LIMIT :pageSize
        """
    )
    suspend fun listDunningCycleExecution(
        dunningCycleId: Long? = null,
        serviceId: UUID? = null,
        pageIndex: Int? = 1,
        pageSize: Int? = 1000
    ): List<DunningCycleExecutionResponse>

    @NewSpan
    @Query(
        """
            SELECT
                count(*)
            FROM
                dunning_cycle_executions               
            WHERE
                (:dunningCycleId IS NULL OR dunning_cycle_id = :dunningCycleId)
                AND (:serviceId IS NULL OR service_id::uuid = :serviceId)
        """
    )
    suspend fun totalCountDunningCycleExecution(
        dunningCycleId: Long? = null,
        serviceId: UUID? = null
    ): Long
}
