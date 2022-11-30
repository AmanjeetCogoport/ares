package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.PaymentMigrationEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.math.BigDecimal

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface PaymentMigrationRepository : CoroutineCrudRepository<PaymentMigrationEntity, Long> {
    @WithSpan
    @Query(
        """
            SELECT EXISTS
            (SELECT p.id FROM payments p  inner join account_utilizations au on (au.document_value = p.payment_num_value and p.sage_organization_id = au.sage_organization_id
            and p.acc_mode=au.acc_mode)
            WHERE p.migrated=true and p.payment_num_value=:paymentNumValue
            and p.acc_mode  = :accMode::account_mode  and p."payment_code"=:paymentCode::payment_code
            and au.acc_type=:accType::account_type
            )
        """
    )
    suspend fun checkPaymentExists(paymentNumValue: String, accMode: String, paymentCode: String, accType: String): Boolean

    @WithSpan
    @Query(
        """
            select exists (select j.id from journal_vouchers j 
                inner join account_utilizations au on (au.document_value = j.jv_num and j.acc_mode=au.acc_mode) 
                where j.created_by = '2f5e5152-03f4-4ea8-a3db-a6eff388161b' 
                and j.jv_num =:jvNum 
                and j.acc_mode =:accMode::account_mode
                and au.acc_type=:accType::account_type)
        """
    )
    suspend fun checkJVExists(jvNum: String, accMode: String, accType: String): Boolean

    @WithSpan
    @Query(
        """
        select id from payments where payment_num_value=:paymentNumValue  
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

    @WithSpan
    @Query(
        """
            select * from account_utilizations 
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

    @WithSpan
    @Query(
        """
            select exists (select id from settlements 
            where source_id=:sourceId 
            and destination_id =:destinationId
            and led_amount =:ledgerAmount)
        """
    )
    suspend fun checkDuplicateForSettlements(sourceId: Long, destinationId: Long, ledgerAmount: BigDecimal): Boolean

    @WithSpan
    @Query(
        """
            select * from account_utilizations 
            where document_value= :documentNumber 
            and acc_mode =:accMode::account_mode limit 1 
        """
    )
    suspend fun getDestinationIdForAr(
        documentNumber: String,
        accMode: String
    ): Long
}
