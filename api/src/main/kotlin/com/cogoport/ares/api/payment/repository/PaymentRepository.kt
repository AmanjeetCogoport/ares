package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.response.PaymentDocumentStatusForPayments
import com.cogoport.ares.model.settlement.SageStatusResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface PaymentRepository : CoroutineCrudRepository<Payment, Long> {

    @NewSpan
    @Query(
        """
             select id,entity_code,org_serial_id,sage_organization_id,organization_id,organization_name,
             tagged_organization_id, trade_party_mapping_id, acc_code,acc_mode,sign_flag,currency,amount,led_currency,led_amount,pay_mode,narration,
             trans_ref_number,ref_payment_id,transaction_date::timestamp as transaction_date,is_posted,is_deleted,created_at,updated_at,
             cogo_account_no,ref_account_no,payment_code,bank_name,payment_num,payment_num_value,exchange_rate,bank_id, migrated,bank_pay_amount, payment_document_status,
             sage_num_value, created_by, updated_by
             from payments where id =:id and deleted_at is null
        """
    )
    suspend fun findByPaymentId(id: Long?): Payment

    @NewSpan
    @Query(
        """
        select exists( select id from payments where organization_id = :organizationId and trans_ref_number = :transRefNumber and deleted_at is null)
    """
    )
    suspend fun isTransRefNumberExists(organizationId: UUID?, transRefNumber: String): Boolean

    @NewSpan
    @Query(
        """
            UPDATE payments SET deleted_at = NOW(),updated_at = NOW() WHERE id = :paymentId
        """
    )
    suspend fun deletePayment(paymentId: Long?)

    @NewSpan
    @Query(
        """
            SELECT id,entity_code,org_serial_id,sage_organization_id,organization_id,organization_name,
             tagged_organization_id, trade_party_mapping_id, acc_code,acc_mode,sign_flag,currency,amount,led_currency,led_amount,pay_mode,narration,
             trans_ref_number,ref_payment_id,transaction_date::timestamp as transaction_date,is_posted,is_deleted,created_at,updated_at,
             cogo_account_no,ref_account_no,payment_code,bank_name,payment_num,payment_num_value,exchange_rate,bank_id, migrated,bank_pay_amount, payment_document_status, 
             sage_num_value,created_by, updated_by
             FROM payments WHERE trans_ref_number = :transRefNumber and deleted_at is null
        """
    )
    suspend fun findByTransRef(transRefNumber: String?): List<Payment>

    @NewSpan
    @Query(
        """
            SELECT id FROM payments WHERE payment_num = :paymentNum and payment_code::varchar = :paymentCode and deleted_at is null  
        """
    )
    suspend fun findByPaymentNumAndPaymentCode(paymentNum: Long?, paymentCode: PaymentCode): Long

    @NewSpan
    @Query(
        """
            SELECT trans_ref_number FROM payments WHERE payment_num = :paymentNum and acc_mode = 'AR' and deleted_at is null
        """
    )
    suspend fun findTransRefNumByPaymentNum(paymentNum: Long?): String

    @NewSpan
    @Query(
        """
            UPDATE 
                payments 
            SET 
                payment_document_status = :paymentDocumentStatus, updated_at = NOW(), updated_by = :performedBy
            WHERE 
                id = :id
            """
    )
    suspend fun updatePaymentDocumentStatus(id: Long, paymentDocumentStatus: PaymentDocumentStatus, performedBy: UUID)

    @NewSpan
    @Query(
        """
            SELECT 
                payment_document_status, 
                array_agg(id) AS payment_ids
            FROM 
                payments
            GROUP BY 
                payment_document_status
        """
    )
    suspend fun getPaymentDocumentStatusWiseIds(): List<PaymentDocumentStatusForPayments>

    @NewSpan
    @Query(
        """
            UPDATE payments SET sage_num_value = :sageNumValue WHERE id = :id
        """
    )
    suspend fun updateSagePaymentNumValue(id: Long, sageNumValue: String)

    @NewSpan
    @Query(
        """
            SELECT
                sage_num_value,
                payment_document_status
            FROM
                payments
            WHERE
                payment_num_value = :paymentNumValue 

        """
    )
    suspend fun getSageDocumentNumberAndStatusByPaymentNumValue(paymentNumValue: String): SageStatusResponse
}
