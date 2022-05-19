package com.cogoport.ares.payment.repository

import com.cogoport.ares.payment.entity.AccountUtilization
import com.cogoport.ares.payment.entity.AgeingBucket
import io.micronaut.data.annotation.Query
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository
interface AccountUtilizationRepository : CoroutineCrudRepository<AccountUtilization, Long> {
    @Query(
        """select case when due_date  >= now() then 'Not Due'
             when (now()::date - due_date ) between 1 and 30 then '1-30'
             when (now()::date - due_date ) between 31 and 60 then '31-60'
             when (now()::date - due_date ) between 61 and 90 then '61-90'
             when (now()::date - due_date ) between 91 and 180 then '91_180'
             when (now()::date - due_date ) between 181 and 365 then '181_365'
             end as ageing_duration,
             zone,
             sum( amount_loc) as amount
             from account_utilizations
             where (zone = :zone OR :zone is null)
             group by ageing_duration, zone
             order by 1
          """
    )
    suspend fun getReceivableByAge(zone: String?): MutableList<AgeingBucket>
}
