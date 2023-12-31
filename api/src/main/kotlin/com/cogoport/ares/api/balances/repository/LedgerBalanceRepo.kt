package com.cogoport.ares.api.balances.repository

import com.cogoport.ares.api.balances.entity.LedgerBalance
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDate

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface LedgerBalanceRepo : CoroutineCrudRepository<LedgerBalance, Long> {

    suspend fun saveAll(ledgerBalanceList: List<LedgerBalance>): List<LedgerBalance>

    @Query(
        """
            SELECT
                *
            FROM
                ledger_balances
            WHERE 
            entity_code = :entityCode
            AND balance_date = :date::DATE
            ORDER BY
                CASE WHEN :sortType = 'ASC' AND :sortField = 'balanceAmount' THEN balance_amount END ASC,
                CASE WHEN :sortType = 'DESC' AND :sortField = 'balanceAmount' THEN balance_amount END DESC
            OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize))
            LIMIT :pageSize
        """
    )
    suspend fun listLedgerBalances(query: String?, entityCode: Int, date: LocalDate, pageIndex: Int, pageSize: Int, sortField: String, sortType: String): List<LedgerBalance>

    @Query(
        """
            SELECT
                COALESCE(COUNT(*), 0)
            FROM
                ledger_balances
            WHERE 
            entity_code = :entityCode
            AND balance_date = :date::DATE
        """
    )
    suspend fun countLedgerBalances(query: String?, entityCode: Int, date: LocalDate): Long
}
