package com.cogoport.ares.payment.repository

import com.cogoport.ares.payment.entity.Payment
import com.cogoport.ares.payment.model.AgeingBucket
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface PaymentRepository : CoroutineCrudRepository<Payment, Long> {

    suspend fun listOrderByCreatedAtDesc(): MutableList<Payment>

    @Query(
        """
             select case when invoice_date + credit_days > now() then 'Not Due'
             when (now()::date - (invoice_date + credit_days)) between 1 and 30 then '1-30'
             when (now()::date - (invoice_date + credit_days)) between 31 and 60 then '31-60'
             when (now()::date - (invoice_date + credit_days)) between 61 and 90 then '61-90'
             when (now()::date - (invoice_date + credit_days)) > 90 then '>90' 
             end as ageing_duration, 
             sum(exchange_rate * grand_total) as amount,
             case when invoice_date + credit_days > now() then 'not_due'
             when (now()::date - (invoice_date + credit_days)) between 1 and 30 then '1_30'
             when (now()::date - (invoice_date + credit_days)) between 31 and 60 then '31_60' 
             when (now()::date - (invoice_date + credit_days)) between 61 and 90 then '61_90'
             when (now()::date - (invoice_date + credit_days)) > 90 then '>90' 
             end as ageing_duration_key
              from invoices
             group by ageing_duration, ageing_duration_key
             order by 1
         """

    )
    suspend fun getAgeingBucket(): List<AgeingBucket>
}
