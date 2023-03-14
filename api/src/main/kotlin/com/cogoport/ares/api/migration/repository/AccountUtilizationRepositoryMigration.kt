package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.AccountUtilizationMigration
import com.cogoport.ares.api.migration.model.PayLocUpdateResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.math.BigDecimal

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepositoryMigration : CoroutineCrudRepository<AccountUtilizationMigration, Long> {

    @NewSpan
    @Query(
        """
            SELECT 
            pay_loc 
            FROM account_utilizations 
            WHERE document_value = :documentValue
            AND acc_mode = :accMode::account_mode
        """
    )
    fun getRecordFromAccountUtilization(
        documentValue: String,
        amountLedger: BigDecimal,
        accMode: String
    ): BigDecimal?

    @NewSpan
    @Query(
        """
            UPDATE account_utilizations 
            SET pay_loc = :payLedger 
            ,pay_curr = :payCurrency
            WHERE 
            document_value = :documentValue
            AND
            acc_mode = :accMode::account_mode
         """
    )
    fun updateUtilizationAmount(
        documentValue: String,
        amountLedger: BigDecimal,
        payLedger: BigDecimal,
        payCurrency: BigDecimal,
        accMode: String
    )

    @NewSpan
    @Query(
        """
            select acc_type,document_no from account_utilizations 
            WHERE 
            document_value = :documentValue
            AND
            acc_mode = :accMode::account_mode
        """
    )
    fun getAccType(
        documentValue: String,
        amountLedger: BigDecimal,
        accMode: String
    ): PayLocUpdateResponse
}
