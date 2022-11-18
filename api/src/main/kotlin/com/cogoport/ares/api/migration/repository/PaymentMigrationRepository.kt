package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.PaymentMigrationEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.math.BigDecimal

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface PaymentMigrationRepository : CoroutineCrudRepository<PaymentMigrationEntity, Long> {
    @Query(
        """
            SELECT EXISTS
            (SELECT id FROM payments p WHERE migrated=true and payment_num_value=:paymentNumValue
            and acc_mode  = :accMode::account_mode  and "payment_code"=:paymentCode::payment_code)
        """
    )
    suspend fun checkPaymentExists(paymentNumValue: String, accMode: String, paymentCode: String): Boolean

    @Query(
        """
            select exists (select id from journal_vouchers where jv_num =:jvNum and acc_mode =:accMode::account_mode)
        """
    )
    suspend fun checkJVExists(jvNum: String, accMode: String): Boolean

    @Query(
        """
        select id from payments where payment_num_value=:paymentNumValue  and acc_mode  =:accMode::account_mode and "payment_code"=:paymentCode::payment_code   
     """
    )
    suspend fun getPaymentId(paymentNumValue: String, accMode: String, paymentCode: String): Long

    @Query(
        """
            select document_no from account_utilizations where document_value=:documentNumber and acc_mode =:accMode::account_mode
        """
    )
    suspend fun getDestinationId(documentNumber: String, accMode: String): Long

    @Query(
        """
            select exists (select id from settlements 
            where source_id=:sourceId 
            and destination_id =:destinationId
            and led_amount =:ledgerAmount)
        """
    )
    suspend fun checkDuplicateForSettlements(sourceId: Long, destinationId: Long, ledgerAmount: BigDecimal): Boolean
}
