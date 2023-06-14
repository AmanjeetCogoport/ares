package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.DunningCycle
import com.cogoport.ares.model.dunning.response.DunningCycleResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.*

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface DunningCycleRepo : CoroutineCrudRepository<DunningCycle, Long> {

    @NewSpan
    @Query(
        """
            SELECT
                *
            FROM
                dunning_cycles
            WHERE
                (:query IS NULL OR name ILIKE :query) AND
                (:status IS NULL OR is_active = :status)
            ORDER BY
                :sortBy :sortType
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
            LIMIT :pageSize
        """
    )
    suspend fun listDunningCycleExecution(
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
                (:status IS NULL OR is_active = :status)
        """
    )
    suspend fun totalCountDunningCycleExecution(
        query: String?,
        status: Boolean?,
    ): Long

    @NewSpan
    @Query(
        """
             UPDATE dunning_cycles
                SET deleted_at = CASE
                   WHEN :actionType = 'DELETE' THEN NOW()
                   ELSE deleted_at
               END,
            is_active = CASE
                   WHEN :actionType != 'DELETE' THEN NOT is_active
                   ELSE is_active
               END,
            updated_at = NOW(),
            updated_by = :updatedBy::UUID
            WHERE id = :id
        """
    )
    suspend fun deleteOrUpdateStatusCycle(
        id: Long,
        updatedBy: UUID,
        actionType: String
    )
}
