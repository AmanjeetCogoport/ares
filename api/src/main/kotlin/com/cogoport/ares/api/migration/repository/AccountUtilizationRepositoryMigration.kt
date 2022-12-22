package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.AccountUtilizationMigration
import com.cogoport.ares.api.migration.model.PayLocUpdateResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.math.BigDecimal

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepositoryMigration : CoroutineCrudRepository<AccountUtilizationMigration, Long> {

    @Query(
        """
            SELECT 
            pay_loc 
            FROM account_utilizations 
            WHERE document_value = :documentValue
            AND sage_organization_id = :sageOrganizationId
            AND amount_loc = :amountLedger
            AND migrated = true
        """
    )
    fun getRecordFromAccountUtilization(
        documentValue: String,
        sageOrganizationId: String,
        amountLedger: BigDecimal
    ): BigDecimal?

    @Query(
        """
            UPDATE account_utilizations 
            SET pay_loc = :payLedger 
            ,pay_curr = :payCurrency
            WHERE 
            document_value = :documentValue
            AND
            sage_organization_id = :sageOrganizationId
            AND
            amount_loc = :amountLedger
            AND
            migrated = true
        """
    )
    fun updateUtilizationAmount(
        documentValue: String,
        sageOrganizationId: String,
        amountLedger: BigDecimal,
        payLedger: BigDecimal,
        payCurrency: BigDecimal
    )

    @Query(
        """
            select acc_type,document_no from account_utilizations 
            WHERE 
            document_value = :documentValue
            AND
            sage_organization_id = :sageOrganizationId
            AND
            amount_loc = :amountLedger
            AND
            migrated = true
        """
    )
    fun getAccType(
        documentValue: String,
        sageOrganizationId: String,
        amountLedger: BigDecimal,
    ): PayLocUpdateResponse
}