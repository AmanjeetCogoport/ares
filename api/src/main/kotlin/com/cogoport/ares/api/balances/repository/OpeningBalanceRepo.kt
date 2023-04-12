package com.cogoport.ares.api.balances.repository

import com.cogoport.ares.api.balances.entity.OpeningBalance
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.Date
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface OpeningBalanceRepo : CoroutineCrudRepository<OpeningBalance, Long> {

    suspend fun saveAll(openingBalanceList: List<OpeningBalance>): List<OpeningBalance>

    @Query(
        """
            SELECT
                *
            FROM
                opening_balances
            WHERE 
            (:query IS NULL OR text ILIKE :query)
            AND entity_id = :entityId::UUID
            AND data = :date::DATE
            ORDER BY
                created_at DESC OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
            LIMIT :pageSize
        """
    )
    suspend fun listOpeningBalances(query: String?, entityId: UUID, date: Date): List<OpeningBalance>?

    @Query(
        """
            SELECT
                COALESCE(COUNT(*), 0)
            FROM
                opening_balances
            WHERE 
            (:query IS NULL OR text ILIKE :query)
            AND entity_id = :entityId::UUID
            AND data = :date::DATE
        """
    )
    suspend fun countOpeningBalances(query: String?, entityId: UUID, date: Date): Long
}
