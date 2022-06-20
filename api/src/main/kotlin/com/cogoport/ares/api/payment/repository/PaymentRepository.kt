package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.Payment
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface PaymentRepository : CoroutineCrudRepository<Payment, Long> {

    suspend fun listOrderByCreatedAtDesc(): MutableList<Payment>

    @Query(
        """
             select id,entity_code,org_serial_id,sage_organization_id,organization_id,organization_name,
             acc_code,acc_mode,sign_flag,currency,amount,led_currency,led_amount,pay_mode,narration,
             trans_ref_number,ref_payment_id,transaction_date::timestamp as transaction_date,is_posted,is_deleted,created_at,updated_at,
             cogo_account_no,ref_account_no,payment_code,bank_name,payment_num,payment_num_value,exchange_rate
             from payments where id =:id 
        """
    )
    suspend fun findByPaymentId(id: Long?): Payment
}
