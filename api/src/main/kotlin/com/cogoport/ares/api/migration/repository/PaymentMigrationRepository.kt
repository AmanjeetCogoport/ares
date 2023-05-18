package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.JvResponse
import com.cogoport.ares.api.migration.entity.PaymentMigrationEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.math.BigDecimal

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface PaymentMigrationRepository : CoroutineCrudRepository<PaymentMigrationEntity, Long> {
    @NewSpan
    @Query(
        """
            SELECT EXISTS
            (SELECT p.id FROM payments p  inner join account_utilizations au on (au.document_value = p.payment_num_value and p.sage_organization_id = au.sage_organization_id
            and p.acc_mode=au.acc_mode)
            WHERE p.migrated=true and p.sage_ref_number = :paymentNumValue
            and p.acc_mode  = :accMode::account_mode  and p."payment_code"=:paymentCode::payment_code
            and au.acc_type=:accType::account_type
            )
        """
    )
    suspend fun checkPaymentExists(paymentNumValue: String, accMode: String, paymentCode: String, accType: String): Boolean

    @NewSpan
    @Query(
        """
            select document_no as jvId,id as accountUtilizationId, amount_loc as amountLedger,pay_loc as payLedger,
            led_currency as ledgerCurrency, updated_at as updated_at from account_utilizations au 
            where document_no = :jvId and document_value = :jvNum
        """
    )
    suspend fun checkJVExists(jvNum: String, jvId: Long): JvResponse?

    @NewSpan
    @Query(
        """
        select payment_num from payments where payment_num_value=:paymentNumValue  
        and acc_mode  =:accMode::account_mode 
        and "payment_code"=:paymentCode::payment_code
        and sage_organization_id = :sageOrganizationId limit 1     
     """
    )
    suspend fun getPaymentId(
        paymentNumValue: String,
        accMode: String,
        paymentCode: String,
        sageOrganizationId: String
    ): Long

    @NewSpan
    @Query(
        """
            select document_no from account_utilizations 
            where document_value= :documentNumber 
            and acc_mode =:accMode::account_mode 
            and sage_organization_id =:sageOrganizationId limit 1 
        """
    )
    suspend fun getDestinationId(
        documentNumber: String,
        accMode: String,
        sageOrganizationId: String
    ): Long

    @NewSpan
    @Query(
        """
            select exists (select id from settlements 
            where source_id=:sourceId 
            and destination_id =:destinationId
            and led_amount =:ledgerAmount)
        """
    )
    suspend fun checkDuplicateForSettlements(sourceId: Long, destinationId: Long, ledgerAmount: BigDecimal): Boolean

    @NewSpan
    @Query(
        """
            select document_no from account_utilizations 
            where document_value= :documentNumber 
            and acc_mode =:accMode::account_mode limit 1 
        """
    )
    suspend fun getDestinationIdForAr(
        documentNumber: String,
        accMode: String
    ): Long

    @NewSpan
    @Query(
        """
            select payment_num from payments where payment_num_value=:paymentNumValue  
            and acc_mode  =:accMode::account_mode 
            and acc_code=:accCode
            and sage_organization_id = :sageOrganizationId limit 1 
        """
    )
    suspend fun getPaymentIdWithoutPayCode(
        paymentNumValue: String,
        accMode: String,
        accCode: String,
        sageOrganizationId: String
    ): Long

    @Query(
        """
            select id from journal_vouchers where sage_unique_id = :sageUniqueId and jv_num = :jvNum
        """
    )
    suspend fun checkJVWithNoBpr(
        sageUniqueId: String,
        jvNum: String
    ): Long?

    @NewSpan
    @Query(
        """
            SELECT id FROM payments WHERE sage_ref_number = :SageRefNum AND migrated = TRUE 
        """
    )
    suspend fun getPaymentFromSageRefNum(
        SageRefNum: String
    ): Long

    @NewSpan
    @Query(
        """
            UPDATE payments SET sage_ref_number = :sageRefNumber WHERE id = :id AND migrated = TRUE
        """
    )
    suspend fun updateSageRefNum(id: Long, sageRefNumber: String): Long
}
