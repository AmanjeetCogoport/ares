package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.AccountUtilizationMigration
import com.cogoport.ares.api.migration.model.PayLocUpdateResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
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

    @Query(
        """
        SELECT EXISTS (
            SELECT id FROM 
            account_utilizations WHERE 
            document_value = :documentValue 
            AND sage_organization_id = :sageOrganizationId 
            AND migrated = true    
        )
    """
    )
    fun checkIfNewRecordIsPresent(documentValue: String, sageOrganizationId: String): Boolean

    @NewSpan
    @Query(
        """
            update account_utilizations 
            set 
            pay_curr = :payCurr
            ,pay_loc = :payLoc
            ,updated_at = now()
            where id = :id
        """
    )
    fun updateJVUtilizationAmount(
        id: Long,
        payCurr: BigDecimal,
        payLoc: BigDecimal
    )

    @NewSpan
    @Query(
        """
            UPDATE settlements 
            SET amount = amount + :amount, led_amount = led_amount + :ledAmount 
            WHERE source_id = :sourceId AND destination_id = :destinationId AND deleted_at is null AND source_type in ('PAY', 'PCN')
        """
    )
    fun updateSettlementAmount(
        destinationId: Long,
        sourceId: Long,
        amount: BigDecimal,
        ledAmount: BigDecimal
    )

    @NewSpan
    @Query(
        """
            UPDATE account_utilizations SET  pay_curr = pay_curr + :payCurr, pay_loc = pay_loc + :payLoc
            WHERE id = :id
        """
    )
    fun updateAccountUtilizationsAmount(
        id: Long,
        payCurr: BigDecimal,
        payLoc: BigDecimal
    )
    @NewSpan
    @Query(
        """ 
            SELECT 
                *
            FROM 
                account_utilizations 
            WHERE 
                document_no = :documentNo
            AND document_status != 'DELETED'::document_status
            AND (:accType is null OR acc_type = :accType::account_type) 
            AND (:accMode is null OR acc_mode = :accMode::account_mode) 
            AND deleted_at is null AND migrated = false
            AND is_void = false
        """
    )
    suspend fun findNonMigratedRecord(documentNo: Long, accType: String? = null, accMode: String? = null): AccountUtilization?
}
