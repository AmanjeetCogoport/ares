package com.cogoport.ares.api.migration.repository

import com.cogoport.ares.api.migration.entity.JvResponse
import com.cogoport.ares.api.migration.entity.PaymentMigrationEntity
import com.cogoport.ares.api.migration.model.MismatchedAmountEntry
import com.cogoport.ares.api.migration.model.PaymentDetails
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
            SELECT id FROM payments WHERE sage_ref_number = :sageRefNum AND migrated = TRUE 
        """
    )
    suspend fun getPaymentFromSageRefNum(
        sageRefNum: String
    ): Long

    @NewSpan
    @Query(
        """
            UPDATE payments SET sage_ref_number = :sageRefNumber WHERE id = :id AND migrated = TRUE
        """
    )
    suspend fun updateSageRefNum(id: Long, sageRefNumber: String): Long

    @NewSpan
    @Query(
        """
            select au.id ,payment_num, trans_ref_number, pvm.document_no, p.amount, (au.amount_curr - au.pay_curr) as unutilised_amount
            from payments p
            LEFT JOIN account_utilizations au on au.document_no = p.payment_num and au.acc_mode = 'AP'
            Left JOIN payment_invoice_mapping pvm on pvm.payment_id = p.id
            where p.payment_num in (:paymentNum) and pvm.mapping_type = 'BILL' and pvm.account_mode = 'AP' and p.deleted_at is null
            and pvm.deleted_at is null
            order by p.created_at desc
        """
    )
    suspend fun paymentDetailsByPaymentNum(paymentNum: List<String>): List<PaymentDetails>

    @NewSpan
    @Query(
        """
            select p.id, p.amount, p.led_amount, au.document_no ,  au.document_value , au.amount_curr,  au.amount_loc
            from payments as p  
            inner join  account_utilizations as au on p.payment_num = au.document_no and p.payment_num_value = au.document_value
            where p.amount != au.amount_curr and p.led_amount != au.amount_loc
            and p.acc_mode ='AP' and  au.acc_mode ='AP' and au.acc_type = 'PAY'
            and p.migrated = false and au.migrated = false and (p.deleted_at is null or au.deleted_at is null) and p.id = :id
            order by p.transaction_date desc
        """
    )
    suspend fun getMismatchLspPaymentCheck(id: Long): MismatchedAmountEntry

    @NewSpan
    @Query(
        """
            UPDATE account_utilizations
            SET amount_curr = :amount,
            pay_curr = :amount, 
            amount_loc = :ledAmount, 
            pay_loc = :ledAmount
            WHERE document_no = :documentNo and document_value = :documentValue and acc_mode ='AP' and acc_type = 'PAY'
        """
    )
    suspend fun updateMismatchLspPaymentsCheck(amount: BigDecimal, ledAmount: BigDecimal, documentNo: Long?, documentValue: String?): Long
}
