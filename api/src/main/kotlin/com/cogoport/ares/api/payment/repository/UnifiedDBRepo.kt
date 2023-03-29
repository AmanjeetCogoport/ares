package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.payment.entity.AccountUtilization
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import io.micronaut.transaction.annotation.TransactionalAdvice

@TransactionalAdvice(AresConstants.UNIFIED)
@R2dbcRepository(value = AresConstants.UNIFIED, dialect = Dialect.POSTGRES)
interface UnifiedDBRepo : CoroutineCrudRepository<AccountUtilization, Long> {

    @NewSpan
    @Query(
        """
            SELECT inv.id FROM plutus.invoices inv 
            LEFT JOIN ares.account_utilizations ac 
            ON inv.id = ac.document_no AND ac.acc_mode = 'AR'
            WHERE ac.id IS NULL AND inv.status NOT IN ('FINANCE_REJECTED', 'IRN_CANCELLED', 'CONSOLIDATED');
        """
    )
    suspend fun getInvoicesNotPresentInAres(): List<Long>?

    @NewSpan
    @Query(
        """
            SELECT inv.id FROM ares.account_utilizations au 
            INNER JOIN plutus.invoices inv ON inv.id = au.document_no AND acc_type IN ('SINV', 'SCN')
            WHERE inv.grand_total != au.amount_curr OR inv.ledger_total != au.amount_loc;
        """
    )
    suspend fun getInvoicesAmountMismatch(): List<Long>?

    @NewSpan
    @Query(
        """
            SELECT ac.id FROM ares.account_utilizations ac 
            LEFT JOIN plutus.invoices inv ON inv.id = ac.document_no 
            WHERE inv.id IS NULL AND document_status = 'PROFORMA' 
            AND ac.acc_mode = 'AR' AND ac.acc_type IN ('SINV', 'SCN');
        """
    )
    suspend fun getInvoicesNotPresentInPlutus(): List<Long>?
}
