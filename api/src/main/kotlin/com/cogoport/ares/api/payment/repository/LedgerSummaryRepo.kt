package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.EntityLevelStats
import com.cogoport.ares.api.payment.entity.LedgerSummary
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface LedgerSummaryRepo : CoroutineCrudRepository<LedgerSummary, Long> {
    suspend fun saveAll(ledgerSummaryList: List<LedgerSummary>): List<LedgerSummary>

    @NewSpan
    @Query(
        """
            SELECT
            SUM(invoice_not_due_amount) AS invoice_not_due_amount,
            SUM(invoice_today_amount) AS invoice_today_amount,
            SUM(invoice_thirty_amount) AS invoice_thirty_amount,
            SUM(invoice_sixty_amount) AS invoice_sixty_amount,
            SUM(invoice_ninety_amount) AS invoice_ninety_amount, 
            SUM(invoice_one_eighty_amount) AS invoice_one_eighty_amount,
            SUM(invoice_three_sixty_five_amount) AS invoice_three_sixty_five_amount,
            SUM(invoice_three_sixty_five_plus_amount) AS invoice_three_sixty_five_plus_amount,
            SUM(invoice_not_due_count) AS invoice_not_due_count,
            SUM(invoice_today_count) AS invoice_today_count,
            SUM(invoice_thirty_count) AS invoice_thirty_count,
            SUM(invoice_sixty_count) AS invoice_sixty_count,
            SUM(invoice_ninety_count) AS invoice_ninety_count,
            SUM(invoice_one_eighty_count) AS invoice_one_eighty_count,
            SUM(invoice_three_sixty_five_count) AS invoice_three_sixty_five_count,
            SUM(invoice_three_sixty_five_plus_count) AS invoice_three_sixty_five_plus_count,
            SUM(on_account_not_due_amount) AS on_account_not_due_amount, 
            SUM(on_account_today_amount) AS on_account_today_amount,
            SUM(on_account_thirty_amount) AS on_account_thirty_amount,
            SUM(on_account_sixty_amount) AS on_account_sixty_amount,
            SUM(on_account_ninety_amount) AS on_account_ninety_amount,
            SUM(on_account_one_eighty_amount) AS on_account_one_eighty_amount,
            SUM(on_account_three_sixty_five_amount) AS on_account_three_sixty_five_amount,
            SUM(on_account_three_sixty_five_plus_amount) AS on_account_three_sixty_five_plus_amount,
            SUM(on_account_not_due_count) AS on_account_not_due_count,
            SUM(on_account_today_count) AS on_account_today_count,
            SUM(on_account_thirty_count) AS on_account_thirty_count,
            SUM(on_account_sixty_count) AS on_account_sixty_count,
            SUM(on_account_ninety_count) AS on_account_ninety_count,
            SUM(on_account_one_eighty_count) AS on_account_one_eighty_count,
            SUM(on_account_three_sixty_five_count) AS on_account_three_sixty_five_count,
            SUM(on_account_three_sixty_five_plus_count) AS on_account_three_sixty_five_plus_count,
            SUM(not_due_outstanding) AS not_due_outstanding,
            SUM(today_outstanding) AS today_outstanding,
            SUM(thirty_outstanding) AS thirty_outstanding,
            SUM(sixty_outstanding) AS sixty_outstanding,
            SUM(ninety_outstanding) AS ninety_outstanding,
            SUM(one_eighty_outstanding) AS one_eighty_outstanding,
            SUM(three_sixty_five_outstanding) AS three_sixty_five_outstanding,
            SUM(three_sixty_five_plus_outstanding) AS three_sixty_five_plus_outstanding,
            SUM(total_open_invoice_amount) AS total_open_invoice_amount,
            SUM(total_open_on_account_amount) AS total_open_on_account_amount,
            SUM(total_outstanding) as total_outstanding,
            led_currency,
            entity_code
        FROM ledger_summary where (:entityCodes) is null or entity_code in (:entityCodes)
        group by led_currency, entity_code
        """
    )
    suspend fun getEntityLevelStats(entityCodes: List<Int>): List<EntityLevelStats>?
}
