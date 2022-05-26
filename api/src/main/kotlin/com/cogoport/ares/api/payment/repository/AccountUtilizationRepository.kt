package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.*
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.Date

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepository : CoroutineCrudRepository<AccountUtilization, Long> {

    @Query("select exists(select id from account_utilizations where document_no=:documentNo and acc_type=:accType)")
    suspend fun isDocumentNumberExists(documentNo: Long, accType: String): Boolean

    @Query("delete from account_utilizations where document_no=:documentNo and acc_type=:accType")
    suspend fun deleteInvoiceUtils(documentNo: Long, accType: String): Int

    @Query(
        """
            select case when due_date  >= now() then 'Not Due'
             when (now()::date - due_date ) between 1 and 30 then '1-30'
             when (now()::date - due_date ) between 31 and 60 then '31-60'
             when (now()::date - due_date ) between 61 and 90 then '61-90'
             when (now()::date - due_date ) between 91 and 180 then '91_180'
             when (now()::date - due_date ) between 181 and 365 then '181_365'
             end as ageing_duration,
             zone_code as zone,
             sum( amount_loc) as amount
             from account_utilizations
             where (zone_code = :zone OR :zone is null)
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
            where (:zone is null or zone_code = :zone) and acc_type in ('sinv','sdn','scn','rec') and acc_mode = 'ar'
            group by ageing_duration, ageing_duration_key
            order by ageing_duration
        """

    )
    suspend fun getAgeingBucket(zone: String?): MutableList<OverallAgeingStats>

    @Query(
        """
        select
        coalesce(sum(case when acc_type in ('sinv','sdn','scn') then sign_flag*(amount_loc - pay_loc) else 0 end),0) as open_invoices_amount,
        coalesce(sum(case when acc_type in ('sinv','sdn','scn') then 1 else 0 end),0) as open_invoices_count,
        coalesce(abs(sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end)),0) as open_on_account_payment_amount,
        coalesce(sum(case when acc_type in ('sinv','sdn','scn') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end),0) as total_outstanding_amount,
        (select count(distinct organization_id) from account_utilizations where acc_type in ('sinv','sdn','scn') and (:zone is null or zone_code = :zone)) as organization_count, null as id
        from account_utilizations
        where (:zone is null or zone_code = :zone) and acc_mode = 'ar'
    """
    )
    suspend fun generateOverallStats(zone: String?): OverallStats

    @Query(
        """
        (
            select 'Total' as duration,
            sum(case when acc_type in ('sinv','scn','sdn') then sign_flag*(amount_loc - pay_loc) else 0 end) as receivable_amount,
            abs(sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end)) as collectable_amount
            from account_utilizations
            where (:quarter is null or extract(quarter from transaction_date) = :quarter) and (:zone is null or zone_code = :zone) and acc_mode = 'ar'
        )
        union all
        (
            select trim(to_char(date_trunc('month',transaction_date),'Month')) as duration,
            sum(case when acc_type in ('sinv','scn','sdn') then sign_flag*(amount_loc - pay_loc) else 0 end) as receivable_amount,
            abs(sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end)) as collectable_amount 
            from account_utilizations
            where (:quarter is null or extract(quarter from transaction_date) = :quarter) and (:zone is null or zone_code = :zone) and acc_mode = 'ar'
            group by date_trunc('month',transaction_date)
            order by date_trunc('month',transaction_date)
        )
        """
    )
    suspend fun generateCollectionTrend(zone: String?, quarter: Int?): MutableList<CollectionTrend>

    @Query(
        """
        select to_char(date_trunc('month',transaction_date),'Month') as duration,
        sum(case when acc_type in ('sinv','sdn','scn') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end) as amount
        from account_utilizations
        where (:zone is null or zone_code = :zone) and acc_mode = 'ar' 
        group by date_trunc('month',transaction_date)
        order by date_trunc('month',transaction_date)
        limit 5
        """
    )
    suspend fun generateMonthlyOutstanding(zone: String?): MutableList<Outstanding>
    @Query(
        """
            with x as (select to_char(date_trunc('quarter',transaction_date),'Q')::int as quarter,
            sum(case when acc_type in ('sinv','sdn','scn') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end) as total_outstanding_amount 
            from account_utilizations
            where acc_mode = 'ar' and (:zone is null or zone_code = :zone)
            group by date_trunc('quarter',transaction_date)
            order by date_trunc('quarter',transaction_date) desc)
            select case when x.quarter = 1 then 'Jan - Mar'
            when x.quarter = 2 then 'Apr - Jun'
            when x.quarter = 3 then 'Jul - Sep'
            when x.quarter = 4 then 'Oct - Dec' end as duration,
            x.total_outstanding_amount as amount 
            from x
        """
    )
    suspend fun generateQuarterlyOutstanding(zone: String?): MutableList<Outstanding>
    @Query(
        """
        with X as (select 
            extract(month from date_trunc('month',(:date)::date)) as month,
            sum(case when acc_type in ('sinv','sdn','scn') then sign_flag*(amount_loc - pay_loc) else 0 end) as open_invoice_amount,
            abs(sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end)) as on_account_payment,
            sum(case when acc_type in ('sinv','sdn','scn') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'rec' then sign_flag*(amount_loc - pay_loc) else 0 end) as outstandings,
            sum(case when acc_type in ('sinv','sdn','scn') and date_trunc('month',transaction_date) = date_trunc('month',(:date)::date) then sign_flag*amount_loc end) as total_sales,
            date_part('days',date_trunc('month',current_date::date) + '1 month'::interval - '1 day'::interval) as days
            from account_utilizations
        where :zone is null or zone_code = :zone)
        select X.month, X.open_invoice_amount, X.on_account_payment, X.outstandings, coalesce(X.total_sales,0) as total_sales, X.days,
        coalesce((X.outstandings / X.total_sales) * X.days,0) as value
        from X
        """
    )
    suspend fun generateDailySalesOutstanding(zone: String?, date: String): DailyOutstanding
    @Query(
        """
        with X as (select 
            extract(month from date_trunc('month',(:date)::date)) as month,
            sum(case when acc_type in ('pinv','pdn','pcn') then sign_flag*(amount_loc - pay_loc) else 0 end) as open_invoice_amount,
            abs(sum(case when acc_type = 'pay' then sign_flag*(amount_loc - pay_loc) else 0 end)) as on_account_payment,
            sum(case when acc_type in ('pinv','pdn','pcn') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'pay' then sign_flag*(amount_loc - pay_loc) else 0 end) as outstandings,
            sum(case when acc_type in ('pinv','pdn','pcn') and date_trunc('month',transaction_date) = date_trunc('month',(:date)::date) then sign_flag*amount_loc end) as total_sales,
            date_part('days',date_trunc('month',current_date::date) + '1 month'::interval - '1 day'::interval) as days
            from account_utilizations
        where :zone is null or zone_code = :zone)
        select X.month, X.open_invoice_amount, X.on_account_payment, X.outstandings, coalesce(X.total_sales,0) as total_sales, X.days,
        coalesce((X.outstandings / X.total_sales) * X.days,0) as value
        from X
        """
    )
    suspend fun generateDailyPayablesOutstanding(zone: String?, date: String): DailyOutstanding

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
        organization_id from account_utilizations;
        """
    )
    suspend fun fetchInvoice(zone: String?, orgId: String?, offset: Int?, limit: Int): List<CustomerInvoice>
    @Query(
        """
        select organization_id,organization_name,
        sum(case when due_date > now() then sign_flag * (amount_loc - pay_loc) else 0 end) as not_due_amount,
        sum(case when (now()::date - due_date) between 1 and 30 then sign_flag * (amount_loc - pay_loc) else 0 end) as thirty_amount,
        sum(case when (now()::date - due_date) between 31 and 60 then sign_flag * (amount_loc - pay_loc) else 0 end) as sixty_amount,
        sum(case when (now()::date - due_date) between 61 and 90 then sign_flag * (amount_loc - pay_loc) else 0 end) as ninety_amount,
        sum(case when (now()::date - due_date) between 91 and 180 then sign_flag * (amount_loc - pay_loc) else 0 end) as oneeighty_amount,
        sum(case when (now()::date - due_date) between 180 and 365 then sign_flag * (amount_loc - pay_loc) else 0 end) as threesixfive_amount,
        sum(case when (now()::date - due_date) > 365 then sign_flag * (amount_loc - pay_loc) else 0 end) as threesixfiveplus_amount,
        sum(case when due_date > now() then 1 else 0 end) as not_due_count,
        sum(case when (now()::date - due_date) between 1 and 30 then 1 else 0 end) as thirty_count,
        sum(case when (now()::date - due_date) between 31 and 60 then 1 else 0 end) as sixty_count,
        sum(case when (now()::date - due_date) between 61 and 90 then 1 else 0 end) as ninety_count,
        sum(case when (now()::date - due_date) between 91 and 180 then 1 else 0 end) as oneeighty_count,
        sum(case when (now()::date - due_date) between 180 and 365 then 1 else 0 end) as threesixfive_count,
        sum(case when (now()::date - due_date) > 365 then 1 else 0 end) as threesixfiveplus_count
        from account_utilizations
        where :zone is null or zone_code = :zone
        group by organization_name,organization_id
        order by organization_name
        offset ((:pageLimit * :page) - :pageLimit)
        limit :pageLimit
        """
    )
    suspend fun getOutstandingAgeingBucket(zone: String?, page: Int, pageLimit: Int): List<OutstandingAgeing>
    @Query(
        """
        select organization_name,
        sum(case when acc_type = 'sinv' and amount_loc - pay_loc <> 0 then 1 else 0 end) as open_invoice_count,
        sum(case when acc_type = 'sinv' and amount_curr = amount_loc then amount_loc else 0 end) as invoice_amount_inr,
        sum(case when acc_type = 'sinv' and amount_curr <> amount_loc then amount_curr else 0 end) as invoice_amount_usd,
        sum(case when acc_type = 'rec' then 1 else 0 end) as on_account_payment_count,
        sum(case when acc_type = 'rec' and amount_curr = amount_loc then amount_loc else 0 end) as on_account_payment_inr,
        sum(case when acc_type = 'rec' and amount_curr <> amount_loc then amount_curr else 0 end) as on_account_payment_usd,
        sum(case when acc_type = 'sinv' and amount_curr = amount_loc then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'rec' and amount_curr = amount_loc then sign_flag*(amount_loc - pay_loc) else 0 end) as outstanding_inr,
        sum(case when acc_type = 'sinv' and amount_curr <> amount_loc then sign_flag*(amount_curr - pay_curr) else 0 end) + sum(case when acc_type = 'rec' and amount_curr <> amount_loc then sign_flag*(amount_curr - pay_curr) else 0 end) as outstanding_usd
        from account_utilizations
        where organization_id::varchar = :orgId and (:zone is null or zone_code = :zone)
        group by organization_name
        """
    )
    suspend fun generateOrgOutstanding(orgId: String, zone: String?): OrgOutstanding

}
