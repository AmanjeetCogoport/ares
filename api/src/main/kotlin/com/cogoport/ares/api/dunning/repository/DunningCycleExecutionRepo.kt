package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.DunningCycleExecution
import com.cogoport.ares.model.dunning.enum.DunningCycleType
import com.cogoport.ares.model.dunning.response.DunningCycleExecutionResponse
import com.cogoport.ares.model.payment.ServiceType
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
                dc.is_active as dunningCycleStatus,
                dc.cycle_type as dunningCycleType,
                dce.*
            FROM
                dunning_cycle_executions dce
            JOIN
                dunning_cycle dc on
                dc.id = dce.dunning_cycle_id
            WHERE
                (:query IS NULL OR dc.name ILIKE :query) AND
                (:status IS NULL OR dc.is_active = :status) AND
                (:dunningCycleType IS NULL OR dc.cycle_type = :dunningCycleType) AND
                (:serviceType IS NULL OR dce.filters->serviceTypes IS NULL OR :serviceType IN dce.filters->serviceTypes) 
            ORDER BY
                :sortBy :sortType
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
            LIMIT :pageSize
                
        """
    )
    suspend fun listDunningCycleExecution(
        query: String?,
        status: Boolean?,
        dunningCycleType: DunningCycleType?,
        serviceType: ServiceType?,
        sortBy: String? = "createdAt",
        sortType: String? = "DESC",
        pageIndex: Int? = 1,
        pageSize: Int? = 10
    ): List<DunningCycleExecutionResponse>

    @NewSpan
    @Query(
        """
            SELECT
                COALESCE(COUNT(*), 0)
            FROM
                dunning_cycle_executions dce
            JOIN
                dunning_cycle dc on
                dc.id = dce.dunning_cycle_id
            WHERE
                (:query IS NULL OR dc.name ILIKE :query) AND
                (:status IS NULL OR dc.is_active = :status) AND
                (:dunningCycleType IS NULL OR dc.cycle_type = :dunningCycleType) AND
                (:serviceType IS NULL OR dce.filters->serviceTypes IS NULL OR :serviceType IN dce.filters->serviceTypes) 
        """
    )
    suspend fun totalCountDunningCycleExecution(
        query: String?,
        status: Boolean?,
        dunningCycleType: DunningCycleType?,
        serviceType: ServiceType?
    ): Long

    @NewSpan
    @Query(
        """
             UPDATE dunning_cycle_executions
                SET deleted_at = CASE
                   WHEN :actionType = 'DELETE' THEN NOW()
                   ELSE deleted_at
               END,
            status = CASE
                   WHEN :actionType != 'DELETE' THEN NOT is_active
                   ELSE is_active
               END,
            status = 'CANCELLED',
            updated_at = NOW(),
            updated_by = :updatedBy::UUID
            WHERE id = :id
        """
    )
    suspend fun deleteOrUpdateStatusCycleExecution(
        id: Long,
        updatedBy: UUID,
        actionType: String
    )

    @Query(
        """
            UPDATE dunning_cycle_executions SET status = :status WHERE id = :id
        """
    )
    suspend fun updateStatus(id: Long, status: String)
}
