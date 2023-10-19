package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.PaymentRelatedFields
import com.cogoport.ares.model.payment.PlatformPayment
import com.cogoport.ares.model.payment.response.PaymentDocumentStatusForPayments
import com.cogoport.ares.model.payment.response.PaymentResponse
import com.cogoport.ares.model.payment.response.TransRefNumberResponse
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
             trans_ref_number,ref_payment_id,transaction_date::timestamp as transaction_date,created_at,updated_at,
             cogo_account_no,ref_account_no,payment_code,bank_name,payment_num,payment_num_value,exchange_rate,bank_id, migrated,bank_pay_amount, payment_document_status, created_by, updated_by, sage_ref_number,
             deleted_at,
             pre_migrated_deleted
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
             trans_ref_number,ref_payment_id,transaction_date::timestamp as transaction_date,created_at,updated_at,
             cogo_account_no,ref_account_no,payment_code,bank_name,payment_num,payment_num_value,exchange_rate,bank_id, migrated,bank_pay_amount, payment_document_status, created_by, updated_by, sage_ref_number,
             deleted_at, pre_migrated_deleted
             FROM payments WHERE trans_ref_number = :transRefNumber and deleted_at is null
        """
    )
    suspend fun findByTransRef(transRefNumber: String?): List<Payment>

    @NewSpan
    @Query(
        """
            SELECT * FROM payments WHERE payment_num = :paymentNum and payment_code::varchar = :paymentCode and deleted_at is null  
        """
    )
    suspend fun findByPaymentNumAndPaymentCode(paymentNum: Long?, paymentCode: PaymentCode): Payment?

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
            WHERE 
                    id IN (:paymentIds) 
            AND 
                payment_document_status != 'DELETED'::payment_document_status 
            GROUP BY payment_document_status
        """
    )
    suspend fun getPaymentDocumentStatusWiseIds(paymentIds: List<Long>): List<PaymentDocumentStatusForPayments>?

    @NewSpan
    @Query(
        """
            UPDATE 
                payments 
            SET 
                sage_ref_number = :sageRefNumber, 
                payment_document_status = 'POSTED'::payment_document_status,
                updated_at = NOW(), updated_by = :performedBy 
            WHERE 
                id = :id
        """
    )
    suspend fun updateSagePaymentNumValue(id: Long, sageRefNumber: String, performedBy: UUID)

    @NewSpan
    @Query(
        """
          SELECT EXISTS( SELECT id FROM payments WHERE trans_ref_number = :transRefNumber AND acc_mode::varchar = :accMode 
          and payment_code NOT IN ('CTDS', 'VTDS') AND deleted_at IS NULL)
        """
    )
    suspend fun isARTransRefNumberExists(accMode: String, transRefNumber: String): Boolean

    @NewSpan
    @Query(
        """
          SELECT 
            id, 
            entity_code,
            org_serial_id,
            organization_name,
            cogo_account_no as bank_account_number,
            bank_name,
            acc_code,
            sage_organization_id,
            organization_id,
            currency,
            amount,
            led_currency,
            led_amount,
            updated_by,
            trans_ref_number as utr,
            acc_mode,
            payment_code,
            payment_document_status,
            transaction_date,
            '' as uploaded_by,
            exchange_rate,
            payment_num,
            payment_num_value,
            created_at,
            deleted_at,
            narration,
            bank_id,
            pay_mode,
            created_by
            FROM 
            payments
            WHERE 
            deleted_at IS NULL
            AND
            (:currencyType IS NULL OR currency = :currencyType)
            AND 
            (COALESCE(:entityCodes) IS NULL OR entity_code IN (:entityCodes))
            AND
            (:accMode IS NULL OR acc_mode::VARCHAR = :accMode)
            AND (:startDate IS NULL OR transaction_date::VARCHAR >= :startDate)
            AND (:endDate IS NULL OR transaction_date::VARCHAR <= :endDate)
            AND (:query IS NULL OR organization_name ILIKE :query OR trans_ref_number ILIKE :query OR payment_num_value ILIKE :query)
            AND (:paymentDocumentStatus IS NULL OR payment_document_status::VARCHAR = :paymentDocumentStatus)
            AND (COALESCE(:documentTypes) IS NULL OR payment_code::VARCHAR IN (:documentTypes))
            ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                     CASE WHEN :sortBy = 'transactionDate' THEN EXTRACT(epoch FROM transaction_date)::numeric
                          WHEN :sortBy = 'createdAt' THEN EXTRACT(epoch FROM created_at)::numeric
                          WHEN :sortBy = 'amount' THEN amount
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                     CASE WHEN :sortBy = 'transactionDate' THEN EXTRACT(epoch FROM transaction_date)::numeric
                          WHEN :sortBy = 'createdAt' THEN EXTRACT(epoch FROM created_at)::numeric
                         WHEN :sortBy = 'amount' THEN amount
                    END        
            END 
            Asc
            OFFSET GREATEST(0, ((:page - 1) * :pageLimit))
            LIMIT :pageLimit
        """
    )
    suspend fun getOnAccountList(
        currencyType: String?,
        entityCodes: List<Int?>?,
        accMode: AccMode?,
        startDate: String?,
        endDate: String?,
        query: String?,
        sortType: String?,
        sortBy: String?,
        pageLimit: Int?,
        page: Int?,
        documentTypes: List<String>?,
        paymentDocumentStatus: PaymentDocumentStatus?
    ): List<PaymentResponse>?

    @NewSpan
    @Query(
        """
          SELECT 
          COALESCE(COUNT(*), 0)
          FROM payments
          WHERE 
          deleted_at IS NULL
          AND
          (:currencyType IS NULL OR currency = :currencyType)
          AND 
          (COALESCE(:entityCodes) IS NULL OR entity_code IN (:entityCodes))
          AND
          (:accMode IS NULL OR acc_mode::VARCHAR = :accMode)
          AND (:startDate IS NULL OR transaction_date::VARCHAR >= :startDate)
          AND (:endDate IS NULL OR transaction_date::VARCHAR <= :endDate)
          AND (:query IS NULL OR organization_name ILIKE :query OR trans_ref_number ILIKE :query OR payment_num_value ILIKE :query)
          AND (:paymentDocumentStatus IS NULL OR payment_document_status::VARCHAR = :paymentDocumentStatus)
          AND (COALESCE(:documentTypes) IS NULL OR payment_code::VARCHAR IN (:documentTypes))
        """
    )
    suspend fun getOnAccountListCount(
        currencyType: String?,
        entityCodes: List<Int?>?,
        accMode: AccMode?,
        startDate: String?,
        endDate: String?,
        query: String?,
        documentTypes: List<String>?,
        paymentDocumentStatus: PaymentDocumentStatus?
    ): Int

    @NewSpan
    suspend fun findByPaymentNumValue(paymentNumValue: String): List<Payment>?

    @NewSpan
    suspend fun countByPaymentNumValueEquals(paymentNumValues: String): Int

    @NewSpan
    @Query(
        """
          SELECT * FROM payments WHERE payment_num_value = :paymentNumValues AND entity_code = :entityCode AND acc_mode::varchar = :accMode
        """
    )
    suspend fun getPaymentByPaymentNumValue(paymentNumValues: String, entityCode: Long?, accMode: AccMode): PlatformPayment

    @NewSpan
    @Query(
        """
            SELECT sage_ref_number FROM payments WHERE payment_num_value = :paymentNumValue AND deleted_at IS NULL and payment_document_status != 'DELETED'::payment_document_status LIMIT 1
        """
    )
    suspend fun findBySinglePaymentNumValue(paymentNumValue: String): String?

    @NewSpan
    @Query(
        """
                SELECT
                    p.id
                FROM
                    payments p
                INNER JOIN
                    account_utilizations au on p.payment_num_value = au.document_value and p.payment_num = au.document_no
                WHERE
                    p.acc_mode = 'AP'
                AND 
                    p.deleted_at IS NULL AND au.deleted_at IS NULL
                AND 
                    payment_document_status = 'APPROVED'::payment_document_status
                AND 
                    p.organization_id != '8c7e0382-4f6d-4a32-bb98-d0bf6522fdd8'
                AND
                    p.migrated = FALSE AND au.migrated = FALSE
                AND
                    p.entity_code != 501
                AND 
                    p.transaction_date <= current_date - INTERVAL '3 days'
                AND
                    p.transaction_date >= '2023-07-25'
            """
    )
    suspend fun getPaymentIdsForApprovedPayments(): List<Long>?

    @Query(
        """
            select payment_num, payment_num_value, payment_code 
            from payments
            where acc_mode::varchar = :accMode AND payment_document_status != 'DELETED'::payment_document_status and deleted_at is null and transaction_date < 'Apr 1, 2023'
            and payment_num_value in (:paymentNumValues)
        """
    )
    suspend fun getPaymentRelatedField(accMode: String, paymentNumValues: List<String>): List<PaymentRelatedFields>

    @NewSpan
    @Query(
        """
            update 
            payments 
            set deleted_at = NOW(), pre_migrated_deleted = true
            where payment_num_value in (:paymentNumValue)
        """
    )
    suspend fun deletingApPayments(paymentNumValue: List<String>)

    @NewSpan
    @Query(
        """
            SELECT payment_num, trans_ref_number FROM payments 
            WHERE payment_num IN (:paymentNums) AND acc_mode::varchar = :accMode
            AND organization_id = :orgId::uuid
            AND payment_code::varchar = :paymentCode
            AND deleted_at is null
        """
    )
    suspend fun findTransRefNumByPaymentNums(paymentNums: List<Long>, accMode: String, orgId: String, paymentCode: String): List<TransRefNumberResponse>

    @NewSpan
    @Query(
        """
            SELECT
                *
            FROM 
                payments
            WHERE
                id in (:ids)
        """
    )
    suspend fun getPaymentByIds(
        ids: List<Long>
    ): List<Payment>
}
