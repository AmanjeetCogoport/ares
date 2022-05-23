package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.*
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
    suspend fun getReceivableByAge(zone: String?): MutableList<AgeingBucketZone>
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
            end as ageing_duration_key,
            'INR' as currency
            from account_utilizations
            where :zone is null or zone_code = :zone
            group by ageing_duration, ageing_duration_key
            order by 1
        """

    )
    suspend fun getAgeingBucket(zone: String?): MutableList<OverallAgeingStats>

    @Query(
        """
        select
        coalesce(sum(case when acc_type = 'sinv' then sign_flag*(amount_loc - pay_loc) else 0 end),0) as open_invoices_amount,
        coalesce(sum(case when acc_type = 'sinv' then 1 else 0 end),0) as open_invoices_count,
        coalesce(abs(sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end)),0) as open_on_account_payment_amount,
        coalesce(sum(case when acc_type = 'sinv' then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end),0) as total_outstanding_amount,
        (select count(distinct organization_id) from account_utilizations where acc_type = 'sinv' and :zone is null or zone_code = :zone) as organization_count, null as id
        from account_utilizations
        where :zone is null or zone_code = :zone
    """
    )
    suspend fun generateOverallStats(zone: String?): OverallStats

    @Query(
        """
        (
            select 'Total' as duration,
            sum(case when acc_type = 'sinv' then sign_flag*(amount_loc - pay_loc) else 0 end) as receivable_amount,
            abs(sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end)) as collectable_amount
            from account_utilizations
            where extract(quarter from transaction_date) = '2' and (null is null or zone_code = 'north')
        )
        union all
        (
            select trim(to_char(date_trunc('month',transaction_date),'Month')) as duration,
            sum(case when acc_type = 'sinv' then sign_flag*(amount_loc - pay_loc) else 0 end) as receivable_amount,
            abs(sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end)) as collectable_amount 
            from account_utilizations
            where extract(quarter from transaction_date) = '2' and (null is null or zone_code = 'north')
            group by date_trunc('month',transaction_date)
            order by date_trunc('month',transaction_date)
        )
        """
    )
    suspend fun generateCollectionTrend(zone: String?, quarter: Int?): MutableList<CollectionTrend>

    @Query(
        """
        select to_char(date_trunc('month',transaction_date),'Month') as duration,
        sum(case when acc_type = 'sinv' then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end) as amount
        from account_utilizations
        where :zone is null or zone_code = :zone
        group by date_trunc('month',transaction_date)
        order by date_trunc('month',transaction_date) desc
        limit 5
        """
    )
    suspend fun generateMonthlyOutstanding(zone: String?): MutableList<Outstanding>
    @Query(
        """
            with x as (select to_char(date_trunc('quarter',transaction_date),'Q')::int as quarter,
            sum(case when acc_type = 'sinv' then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end) as total_outstanding_amount from account_utilizations
            where :zone is null or zone_code = :zone
            group by date_trunc('quarter',transaction_date)
            order by date_trunc('quarter',transaction_date) desc)
            select case when x.quarter = 1 then 'Jan - Mar'
            when x.quarter = 2 then 'Apr - Jun'
            when x.quarter = 3 then 'Jul - Sep'
            when x.quarter = 4 then 'Oct - Dec' end as duration,
            x.total_outstanding_amount as amount from x
        """
    )
    suspend fun generateQuarterlyOutstanding(zone: String?): MutableList<Outstanding>
    @Query(
        """
        select trim(to_char(date_trunc('month',transaction_date),'Month')) as month,
        coalesce(
            (
            sum(case when acc_type = 'sinv' then sign_flag*(amount_loc - pay_loc) else 0 end)
            + 
            sum(case when acc_type = 'rec' then 	sign_flag*(amount_loc - pay_loc) else 0 end)
            ) 
            *
            (date_part('days',date_trunc('month',current_date::date) + '1 month'::interval - '1 day'::interval)) 
            /
            sum(case when acc_type = 'sinv' then amount_loc end)
            ,0) as dso_for_the_month
        from account_utilizations
        where (:zone is null or zone_code = :zone) and (:quarter is null or extract(quarter from transaction_date) = :quarter)
        group by date_trunc('month', transaction_date)
        order by date_trunc('month', transaction_date) desc
        """
    )
    suspend fun generateDailySalesOutstanding(zone: String?, quarter: Int?): List<Dso>

    @Query(
        """
        select document_no as invoice_number,
        acc_type as invoice_type,
        null as shipment_id,
        null as shipment_type,
        null as doc_type,
        amount_loc as invoice_amount,
        'INR' as currency,
        amount_loc - pay_loc as balance_amount,
        transaction_date as invoice_date,
        due_date as invoice_due_date,
        case when due_date < now() then now()::date - due_date else 0 end as overdue_days,
        organization_name,
        organization_id from account_utilizations
        where acc_type in ('sinv','pinv') and (:zone is null or zone_code = :zone) and (:orgId is null or organization_id::varchar = :orgId)
        limit :limit offset :offset
        """
    )
    suspend fun fetchInvoice(zone: String?, orgId: String?, offset: Int?, limit: Int): List<CustomerInvoice>
}
