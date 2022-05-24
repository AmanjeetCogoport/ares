package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.model.payment.AgeingBucket
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface PaymentRepository : CoroutineCrudRepository<Payment, Long> {

    suspend fun listOrderByCreatedAtDesc(): MutableList<Payment>

    @Query(
        """
            select case when due_date > now() then 'Not Due'
            when (now()::date - due_date) between 1 and 30 then '1-30'
            when (now()::date - due_date) between 31 and 60 then '31-60'
            when (now()::date - due_date) between 61 and 90 then '61-90'
            when (now()::date - due_date) > 90 then '>90' 
            end as ageing_duration, 
            sum(sign_flag * (amount_loc - pay_loc)) as amount,
            case when due_date > now() then 'not_due'
            when (now()::date - due_date) between 1 and 30 then '1_30'
            when (now()::date - due_date) between 31 and 60 then '31_60' 
            when (now()::date - due_date) between 61 and 90 then '61_90'
            when (now()::date - due_date) > 90 then '>90' 
            end as ageing_duration_key
             from account_utilizations
            group by ageing_duration, ageing_duration_key
            order by 1
        """

    )
    suspend fun getAgeingBucket(): List<AgeingBucket>
}
