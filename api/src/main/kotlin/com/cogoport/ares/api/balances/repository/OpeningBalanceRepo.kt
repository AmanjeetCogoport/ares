package com.cogoport.ares.api.balances.repository

import com.cogoport.ares.api.balances.entity.OpeningBalance
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDate

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
            (:query IS NULL OR ledger_currency ILIKE :query)
            AND entity_code = :entityCode
            AND balance_date = :date::DATE
            ORDER BY
                CASE WHEN :sortType = 'ASC' AND :sortField = 'balanceAmount' THEN balance_amount END ASC,
                CASE WHEN :sortType = 'DESC' AND :sortField = 'balanceAmount' THEN balance_amount END DESC
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
            LIMIT :pageSize
        """
    )
    suspend fun listOpeningBalances(query: String?, entityCode: Int, date: LocalDate, pageIndex: Int, pageSize: Int, sortField: String, sortType: String): List<OpeningBalance>?

    @Query(
        """
            SELECT
                COALESCE(COUNT(*), 0)
            FROM
                opening_balances
            WHERE 
            (:query IS NULL OR ledger_currency ILIKE :query)
            AND entity_code = :entityCode
            AND balance_date = :date::DATE
        """
    )
    suspend fun countOpeningBalances(query: String?, entityCode: Int, date: LocalDate): Long
}
