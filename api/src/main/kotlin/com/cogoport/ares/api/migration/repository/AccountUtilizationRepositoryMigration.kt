package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.AccountUtilizationMigration
import com.cogoport.ares.api.migration.model.PayLocUpdateResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.math.BigDecimal
import java.util.UUID

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
            AND organization_id = :organizationId
        """
    )
    fun getRecordFromAccountUtilization(
        documentValue: String,
        accMode: String,
        organizationId: UUID
    ): BigDecimal?

    @NewSpan
    @Query(
        """
            UPDATE account_utilizations 
            SET pay_loc = :payLedger 
            ,pay_curr = :payCurrency
            ,updated_at = now()
            WHERE 
            document_value = :documentValue
            AND
            acc_mode = :accMode::account_mode
            AND organization_id = :organizationId 
         """
    )
    fun updateUtilizationAmount(
        documentValue: String,
        payLedger: BigDecimal,
        payCurrency: BigDecimal,
        accMode: String,
        organizationId: UUID
    )

    @NewSpan
    @Query(
        """
            select acc_type,document_no from account_utilizations 
            WHERE 
            document_value = :documentValue
            AND
            acc_mode = :accMode::account_mode
            AND organization_id = :organizationId
        """
    )
    fun getAccType(
        documentValue: String,
        accMode: String,
        organizationId: UUID
    ): PayLocUpdateResponse

    @NewSpan
    @Query(
        """
            select payment_num_value from payments 
            WHERE 
            sage_ref_number = :sageRefNumber
            AND
            acc_mode = :accMode::account_mode
            AND organization_id = :organizationId
        """
    )
    fun getPaymentDetails(
        sageRefNumber: String,
        accMode: String,
        organizationId: UUID
    ): String?
}
