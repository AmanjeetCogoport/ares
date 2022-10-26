package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.payment.entity.PaymentData
import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface PaymentRepository : CoroutineCrudRepository<Payment, Long> {

    @Query(
        """
             select id,entity_code,org_serial_id,sage_organization_id,organization_id,organization_name,
             tagged_organization_id, trade_party_mapping_id, acc_code,acc_mode,sign_flag,currency,amount,led_currency,led_amount,pay_mode,narration,
             trans_ref_number,ref_payment_id,transaction_date::timestamp as transaction_date,is_posted,is_deleted,created_at,updated_at,
             cogo_account_no,ref_account_no,payment_code,bank_name,payment_num,payment_num_value,exchange_rate,bank_id, migrated,bank_pay_amount
             from payments where id =:id 
        """
    )
    suspend fun findByPaymentId(id: Long?): Payment

    @Query(
        """
        select exists( select id from payments where organization_id = :organizationId and trans_ref_number = :transRefNumber)
    """
    )
    suspend fun isTransRefNumberExists(organizationId: UUID?, transRefNumber: String): Boolean

    @Query(
        """
            SELECT 
                payment_num as document_no,
                transaction_date::timestamp AS transaction_date, 
                exchange_rate AS exchange_rate
                FROM payments 
                WHERE payment_code::varchar = :paymentCode
                AND payment_num IN (:paymentNums)
        """
    )
    suspend fun findByPaymentNumIn(paymentNums: List<Long>, paymentCode: SettlementType): List<PaymentData>
}