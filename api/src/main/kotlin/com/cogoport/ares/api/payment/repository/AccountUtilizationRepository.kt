package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.AgeingBucketZone
import com.cogoport.ares.api.payment.entity.CollectionTrend
import com.cogoport.ares.api.payment.entity.DailyOutstanding
import com.cogoport.ares.api.payment.entity.OrgOutstanding
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.api.payment.entity.OutstandingAgeing
import com.cogoport.ares.api.payment.entity.OverallAgeingStats
import com.cogoport.ares.api.payment.entity.OverallStats
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.math.BigDecimal

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepository : CoroutineCrudRepository<AccountUtilization, Long> {

    @Query("select exists(select id from account_utilizations where document_no=:documentNo and acc_type=:accType::account_type)")
    suspend fun isDocumentNumberExists(documentNo: Long, accType: String): Boolean

    @Query(
        """select id,document_no,document_value , zone_code,service_type,document_status,entity_code ,
            category,org_serial_id,sage_organization_id,organization_id,organization_name,acc_code,acc_type,acc_mode,
            sign_flag,currency,led_currency,amount_curr,amount_loc,pay_curr,pay_loc,due_date,transaction_date,created_at,
            updated_at from account_utilizations where document_no = :documentNo and acc_type= :accType"""
    )
    suspend fun findRecord(documentNo: Long, accType: String): AccountUtilization

    @Query("delete from account_utilizations where document_no=:documentNo and acc_type=:accType")
    suspend fun deleteInvoiceUtils(documentNo: Long, accType: String): Int
    suspend fun findByDocumentNo(documentNo: Long): AccountUtilization

    @Query(
        """update account_utilizations set 
              pay_curr = :currencyPay , pay_loc =:ledgerPay , modified_at =now() where id=:id"""
    )
    suspend fun updateInvoicePayment(id: Long, currencyPay: BigDecimal, ledgerPay: BigDecimal)

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
             sum(sign_flag * (amount_loc - pay_loc)) as amount
             from account_utilizations
             where (zone_code = :zone OR :zone is null) and acc_mode = 'AR' and acc_type in ('SINV','SCN','SDN','REC') and document_status = 'FINAL'
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
            where (:zone is null or zone_code = :zone) and acc_mode = 'AR' and acc_type in ('SINV','SCN','SDN','REC') and document_status = 'FINAL'
            group by ageing_duration, ageing_duration_key
            order by ageing_duration
        """

    )
    suspend fun getAgeingBucket(zone: String?): MutableList<OverallAgeingStats>

    @Query(
        """
        select
        coalesce(sum(case when acc_type in ('SINV','SDN','SCN') then sign_flag*(amount_loc - pay_loc) else 0 end),0) as open_invoices_amount,
        coalesce(sum(case when acc_type in ('SINV','SDN','SCN') then 1 else 0 end),0) as open_invoices_count,
        coalesce(abs(sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end)),0) as open_on_account_payment_amount,
        coalesce(sum(case when acc_type in ('SINV','SDN','SCN') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end),0) as total_outstanding_amount,
        (select count(distinct organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') and (:zone is null or zone_code = :zone) and doc_status = 'FINAL' and acc_mode = 'AR' ) as organization_count, 
        null as id
        from account_utilizations
        where (:zone is null or zone_code = :zone) and acc_mode = 'AR' and doc_status = 'FINAL'
    """
    )
    suspend fun generateOverallStats(zone: String?): OverallStats

    @Query(
        """
        (
            select 'Total' as duration,
            sum(case when acc_type in ('SINV','SCN','SDN') then sign_flag*(amount_loc - pay_loc) else 0 end) as receivable_amount,
            abs(sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end)) as collectable_amount
            from account_utilizations
            where (:quarter is null or extract(quarter from transaction_date) = :quarter) and (:zone is null or zone_code = :zone) and acc_mode = 'AR' and doc_status = 'FINAL'
        )
        union all
        (
            select trim(to_char(date_trunc('month',transaction_date),'Month')) as duration,
            sum(case when acc_type in ('SINV','SCN','SDN') then sign_flag*(amount_loc - pay_loc) else 0 end) as receivable_amount,
            abs(sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end)) as collectable_amount 
            from account_utilizations
            where (:quarter is null or extract(quarter from transaction_date) = :quarter) and (:zone is null or zone_code = :zone) and acc_mode = 'AR' and doc_status = 'FINAL'
            group by date_trunc('month',transaction_date)
            order by date_trunc('month',transaction_date)
        )
        """
    )
    suspend fun generateCollectionTrend(zone: String?, quarter: Int?): MutableList<CollectionTrend>

    @Query(
        """
        select to_char(date_trunc('month',transaction_date),'Month') as duration,
        sum(case when acc_type in ('SINV','SDN','SCN','REC') then sign_flag*(amount_loc - pay_loc) else 0 end) as amount
        from account_utilizations
        where (:zone is null or zone_code = :zone) and acc_mode = 'AR' and doc_status = 'FINAL'
        group by date_trunc('month',transaction_date)
        order by date_trunc('month',transaction_date)
        limit 5
        """
    )
    suspend fun generateMonthlyOutstanding(zone: String?): MutableList<Outstanding>
    @Query(
        """
            with x as (select to_char(date_trunc('quarter',transaction_date),'Q')::int as quarter,
            sum(case when acc_type in ('SINV','SDN','SCN') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end) as total_outstanding_amount 
            from account_utilizations
            where acc_mode = 'AR' and (:zone is null or zone_code = :zone) and doc_status = 'FINAL'
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
        with X as (
            select 
            extract(month from date_trunc('month',(:date)::date)) as month,
            sum(case when acc_type in ('SINV','SDN','SCN') then sign_flag*(amount_loc - pay_loc) else 0 end) as open_invoice_amount,
            abs(sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end)) as on_account_payment,
            sum(case when acc_type in ('SINV','SDN','SCN') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end) as outstandings,
            sum(case when acc_type in ('SINV','SDN','SCN') and transaction_date >= date_trunc('month',(:date)::date) then sign_flag*amount_loc end) as total_sales,
            date_part('days',(:date)::date) as days
            from account_utilizations
            where (:zone is null or zone_code = :zone) and doc_status = 'FINAL' and acc_mode = 'AR' and transaction_date <= :date::date
            )
        select X.month, coalesce(X.open_invoice_amount,0) as open_invoice_amount, coalesce(X.on_account_payment, 0) as on_account_payment, coalesce(X.outstandings, 0) as outstandings, coalesce(X.total_sales,0) as total_sales, X.days,
        coalesce((X.outstandings / X.total_sales) * X.days,0) as value
        from X
        """
    )
    suspend fun generateDailySalesOutstanding(zone: String?, date: String): DailyOutstanding
    @Query(
        """
        with X as (
            select 
            extract(month from date_trunc('month',(:date)::date)) as month,
            sum(case when acc_type in ('PINV','PDN','PCN') then sign_flag*(amount_loc - pay_loc) else 0 end) as open_invoice_amount,
            abs(sum(case when acc_type = 'PAY' then sign_flag*(amount_loc - pay_loc) else 0 end)) as on_account_payment,
            sum(case when acc_type in ('PINV','PDN','PCN') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'PAY' then sign_flag*(amount_loc - pay_loc) else 0 end) as outstandings,
            sum(case when acc_type in ('PINV','PDN','PCN') and transaction_date >= date_trunc('month',transaction_date) then sign_flag*amount_loc end) as total_sales,
            date_part('days',(:date)::date) as days
            from account_utilizations
            where (:zone is null or zone_code = :zone) and acc_mode = 'AP' and doc_status = 'FINAL' and transaction_date <= :date::date
        )
        select X.month, coalesce(X.open_invoice_amount,0) as open_invoice_amount, coalesce(X.on_account_payment, 0) as on_account_payment, coalesce(X.outstandings, 0) as outstandings, coalesce(X.total_sales,0) as total_sales, X.days,
        coalesce((X.outstandings / X.total_sales) * X.days,0) as value
        from X
        """
    )
    suspend fun generateDailyPayablesOutstanding(zone: String?, date: String): DailyOutstanding
    @Query(
        """
        select organization_id,zone_code,
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
        where organization_name ilike :orgName and (:zone is null or zone_code = :zone) and acc_mode = 'AR' and document_status = 'FINAL' and (:orgId is null or organization_id = :orgId::uuid)
        group by organization_id,zone_code,organization_name
        order by organization_name
        """
    )
    suspend fun getOutstandingAgeingBucket(zone: String?, orgName: String?, orgId: String?, page: Int, pageLimit: Int): List<OutstandingAgeing>
    @Query(
        """
        select organization_id::varchar,organization_name,currency,zone_code,
        sum(case when acc_type <> 'REC' and amount_curr - pay_curr <> 0 then 1 else 0 end) as open_invoices_count,
        sum(case when acc_type <> 'REC' then sign_flag * (amount_curr - pay_curr) else 0 end) as open_invoices_amount,
        sum(case when acc_type = 'REC' and amount_curr - pay_curr <> 0 then 1 else 0 end) as payments_count,
        sum(case when acc_type = 'REC' then  amount_curr - pay_curr else 0 end) as payments_amount,
        sum(sign_flag * (amount_curr - pay_curr)) as outstanding_amount
        from account_utilizations
        where acc_type in ('SINV','SCN','SDN','REC') and acc_mode = 'AR' and document_status = 'FINAL' and organization_id = :orgId::uuid and zone_code = :zone
        group by organization_id, organization_name, currency, zone_code
        """
    )
    suspend fun generateOrgOutstanding(orgId: String, zone: String): List<OrgOutstanding>
}
