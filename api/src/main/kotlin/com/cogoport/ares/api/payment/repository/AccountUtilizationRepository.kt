package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.AgeingBucket
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepository : CoroutineCrudRepository<AccountUtilization, Long> {

    @Query("select exists(select id from account_utilizations where document_no=:documentNo and acc_type=:accType)")
    suspend fun isDocumentNumberExists(documentNo: Long, accType: String): Boolean

    @Query("delete from account_utilizations where document_no=:documentNo and acc_type=:accType")
    suspend fun deleteInvoiceUtils(documentNo: Long, accType: String): Int

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

    suspend fun findByDocumentNo(documentNo: Long): AccountUtilization
}
