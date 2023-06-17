package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.DunningCycle
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
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
        sortBy: String? = "createdAt",
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
            UPDATE dunning_cycles
             SET 
                schedule_rule::JSONB = :scheduleRule
                updated_by = :updatedBy
             WHERE id = :id
        """
    )
    suspend fun updateDunningCycle(
        id: Long,
        scheduleRule: DunningScheduleRule,
        updatedBy: UUID
    )
}
