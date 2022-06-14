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
import com.cogoport.ares.model.payment.DocumentStatus
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.math.BigDecimal
import java.sql.Timestamp

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepository : CoroutineCrudRepository<AccountUtilization, Long> {

    @Query("select exists(select id from account_utilizations where document_no=:documentNo and acc_type=:accType::account_type)")
    suspend fun isDocumentNumberExists(documentNo: Long, accType: String): Boolean

    @Query(
        """select id,document_no,document_value , zone_code,service_type,document_status,entity_code ,
            category,org_serial_id,sage_organization_id,organization_id,organization_name,acc_code,acc_type,acc_mode,
            sign_flag,currency,led_currency,amount_curr,amount_loc,pay_curr,pay_loc,due_date,transaction_date,created_at,
            updated_at from account_utilizations where document_no = :documentNo and (:accType is null or acc_type= :accType::account_type)"""
    )
    suspend fun findRecord(documentNo: Long, accType: String? = null): AccountUtilization?

    @Query("delete from account_utilizations where id=:id")
    suspend fun deleteInvoiceUtils(id: Long): Int
    suspend fun findByDocumentNo(documentNo: Long): AccountUtilization

    @Query(
        """update account_utilizations set 
              pay_curr = pay_curr + :currencyPay , pay_loc =pay_loc + :ledgerPay , updated_at =now() where id=:id"""
    )
    suspend fun updateInvoicePayment(id: Long, currencyPay: BigDecimal, ledgerPay: BigDecimal): Int

    @Query(
        """
            select case when due_date  >= now()::date then 'Not Due'
             when (now()::date - due_date ) between 1 and 30 then '1-30'
             when (now()::date - due_date ) between 31 and 60 then '31-60'
             when (now()::date - due_date ) between 61 and 90 then '61-90'
             when (now()::date - due_date ) between 91 and 180 then '91-180'
             when (now()::date - due_date ) between 181 and 365 then '181-365'
             when (now()::date - due_date ) > 365 then '365+'
             end as ageing_duration,
             zone_code as zone,
             sum(sign_flag * (amount_loc - pay_loc)) as amount
             from account_utilizations
             where (:zone is null or zone_code = :zone) and acc_mode = 'AR' and acc_type in ('SINV','SCN','SDN') and document_status = 'FINAL'
             group by ageing_duration, zone
             order by 1
          """
    )
    suspend fun getReceivableByAge(zone: String?): MutableList<AgeingBucketZone>
    @Query(
        """
            select case when due_date >= now()::date then 'Not Due'
            when (now()::date - due_date) between 1 and 30 then '1-30'
            when (now()::date - due_date) between 31 and 60 then '31-60'
            when (now()::date - due_date) between 61 and 90 then '61-90'
            when (now()::date - due_date) > 90 then '>90' 
            end as ageing_duration, 
            sum(sign_flag * (amount_loc - pay_loc)) as amount,
            'INR' as currency
            from account_utilizations
            where (:zone is null or zone_code = :zone) and acc_mode = 'AR' and acc_type in ('SINV','SCN','SDN') and document_status = 'FINAL'
            group by ageing_duration
            order by ageing_duration
        """

    )
    suspend fun getAgeingBucket(zone: String?): MutableList<OverallAgeingStats>

    @Query(
        """
        select
        coalesce(sum(case when acc_type in ('SINV','SDN','SCN') then sign_flag*(amount_loc - pay_loc) else 0 end),0) as open_invoices_amount,
        coalesce(sum(case when acc_type in ('SINV','SDN','SCN') and (amount_loc - pay_loc <> 0) then 1 else 0 end),0) as open_invoices_count,
        coalesce(abs(sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end)),0) as open_on_account_payment_amount,
        coalesce(sum(sign_flag*(amount_loc - pay_loc)),0) as total_outstanding_amount,
        (select count(distinct organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') and amount_loc - pay_loc <> 0 and (:zone is null or zone_code = :zone) and document_status = 'FINAL' and acc_mode = 'AR' ) as organization_count, 
        null as id
        from account_utilizations
        where (:zone is null or zone_code = :zone) and acc_mode = 'AR' and document_status = 'FINAL'
    """
    )
    suspend fun generateOverallStats(zone: String?): OverallStats

    @Query(
        """
        (
            select 'Total' as duration,
            coalesce(sum(case when acc_type in ('SINV','SCN','SDN') then sign_flag*(amount_loc - pay_loc) else 0 end),0) as receivable_amount,
            coalesce(abs(sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end)),0) as collectable_amount
            from account_utilizations
            where extract(quarter from transaction_date) = :quarter and extract(year from transaction_date) = :year and (:zone is null or zone_code = :zone) and acc_mode = 'AR' and document_status = 'FINAL'
        )
        union all
        (
            select trim(to_char(date_trunc('month',transaction_date),'Month')) as duration,
            coalesce(sum(case when acc_type in ('SINV','SCN','SDN') then sign_flag*(amount_loc - pay_loc) else 0::double precision end),0) as receivable_amount,
            coalesce(abs(sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end)),0) as collectable_amount 
            from account_utilizations
            where extract(quarter from transaction_date) = :quarter and extract(year from transaction_date) = :year and (:zone is null or zone_code = :zone) and acc_mode = 'AR' and document_status = 'FINAL'
            group by date_trunc('month',transaction_date)
            order by date_trunc('month',transaction_date)
        )
        """
    )
    suspend fun generateCollectionTrend(zone: String?, quarter: Int, year: Int): MutableList<CollectionTrend>

    @Query(
        """
        with x as (
	        select to_char(generate_series(CURRENT_DATE - '4 month'::interval, CURRENT_DATE, '1 month'), 'Mon') as month
        ),
        y as (
            select to_char(date_trunc('month',transaction_date),'Mon') as month,
            sum(case when acc_type in ('SINV','SDN','SCN','REC') then sign_flag*(amount_loc - pay_loc) else 0 end) as amount
            from account_utilizations
            where (:zone is null or zone_code = :zone) and acc_mode = 'AR' and document_status = 'FINAL' and date_trunc('month', transaction_date) >= date_trunc('month', CURRENT_DATE - '5 month'::interval)
            group by date_trunc('month',transaction_date)
        )
        select x.month duration, coalesce(y.amount, 0::double precision) as amount from x left join y on y.month = x.month
        """
    )
    suspend fun generateMonthlyOutstanding(zone: String?): MutableList<Outstanding>
    @Query(
        """
            with x as (
                select extract(quarter from generate_series(CURRENT_DATE - '11 month'::interval, CURRENT_DATE, '3 month')) as quarter
            ),
            y as (
                select to_char(date_trunc('quarter',transaction_date),'Q')::int as quarter,
                sum(case when acc_type in ('SINV','SDN','SCN') then sign_flag*(amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'REC' then sign_flag*(amount_loc - pay_loc) else 0 end) as total_outstanding_amount 
                from account_utilizations
                where acc_mode = 'AR' and (:zone is null or zone_code = :zone) and document_status = 'FINAL' and date_trunc('month', transaction_date) >= date_trunc('month',CURRENT_DATE - '11 month'::interval)
                group by date_trunc('quarter',transaction_date)
            )
            select case when x.quarter = 1 then 'Jan - Mar'
            when x.quarter = 2 then 'Apr - Jun'
            when x.quarter = 3 then 'Jul - Sep'
            when x.quarter = 4 then 'Oct - Dec' end as duration,
            coalesce(y.total_outstanding_amount, 0) as amount 
            from x
            left join y on x.quarter = y.quarter
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
            case when extract(month from :date::date) < extract(month from now()::date) then date_part('days',date_trunc('month',(:date::date + '1 month'::interval)) - '1 day'::interval) 
            else date_part('days', :date::date) end as days
            from account_utilizations
            where (:zone is null or zone_code = :zone) and document_status = 'FINAL' and acc_mode = 'AR' and transaction_date <= :date::date
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
            case when extract(month from :date::date) < extract(month from now()::date) then date_part('days',date_trunc('month',(:date::date + '1 month'::interval)) - '1 day'::interval) 
            else date_part('days', :date::date) end as days
            from account_utilizations
            where (:zone is null or zone_code = :zone) and acc_mode = 'AP' and document_status = 'FINAL' and transaction_date <= :date::date
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
        sum(case when due_date >= now()::date then sign_flag * (amount_loc - pay_loc) else 0 end) as not_due_amount,
        sum(case when (now()::date - due_date) between 1 and 30 then sign_flag * (amount_loc - pay_loc) else 0 end) as thirty_amount,
        sum(case when (now()::date - due_date) between 31 and 60 then sign_flag * (amount_loc - pay_loc) else 0 end) as sixty_amount,
        sum(case when (now()::date - due_date) between 61 and 90 then sign_flag * (amount_loc - pay_loc) else 0 end) as ninety_amount,
        sum(case when (now()::date - due_date) between 91 and 180 then sign_flag * (amount_loc - pay_loc) else 0 end) as oneeighty_amount,
        sum(case when (now()::date - due_date) between 180 and 365 then sign_flag * (amount_loc - pay_loc) else 0 end) as threesixfive_amount,
        sum(case when (now()::date - due_date) > 365 then sign_flag * (amount_loc - pay_loc) else 0 end) as threesixfiveplus_amount,
        sum(case when due_date >= now()::date then 1 else 0 end) as not_due_count,
        sum(case when (now()::date - due_date) between 1 and 30 then 1 else 0 end) as thirty_count,
        sum(case when (now()::date - due_date) between 31 and 60 then 1 else 0 end) as sixty_count,
        sum(case when (now()::date - due_date) between 61 and 90 then 1 else 0 end) as ninety_count,
        sum(case when (now()::date - due_date) between 91 and 180 then 1 else 0 end) as oneeighty_count,
        sum(case when (now()::date - due_date) between 180 and 365 then 1 else 0 end) as threesixfive_count,
        sum(case when (now()::date - due_date) > 365 then 1 else 0 end) as threesixfiveplus_count
        from account_utilizations
        where organization_name ilike :queryName and (:zone is null or zone_code = :zone) and acc_mode = 'AR' and due_date is not null and document_status = 'FINAL' and (:orgId is null or organization_id = :orgId::uuid)
        group by organization_id,zone_code,organization_name
        order by organization_name
        """
    )
    suspend fun getOutstandingAgeingBucket(zone: String?, queryName: String?, orgId: String?, page: Int, pageLimit: Int): List<OutstandingAgeing>
    @Query(
        """
        select organization_id::varchar,organization_name,currency,zone_code,
        sum(case when acc_type <> 'REC' and amount_curr - pay_curr <> 0 then 1 else 0 end) as open_invoices_count,
        sum(case when acc_type <> 'REC' then sign_flag * (amount_curr - pay_curr) else 0 end) as open_invoices_amount,
        sum(case when acc_type <> 'REC' then sign_flag * (amount_loc - pay_loc) else 0 end) as open_invoices_led_amount,
        sum(case when acc_type = 'REC' and amount_curr - pay_curr <> 0 then 1 else 0 end) as payments_count,
        sum(case when acc_type = 'REC' then  amount_curr - pay_curr else 0 end) as payments_amount,
        sum(case when acc_type = 'REC' then  amount_loc - pay_loc else 0 end) as payments_led_amount,
        sum(sign_flag * (amount_curr - pay_curr)) as outstanding_amount,
        sum(sign_flag * (amount_loc - pay_loc)) as outstanding_led_amount
        from account_utilizations
        where acc_type in ('SINV','SCN','SDN','REC') and acc_mode = 'AR' and document_status = 'FINAL' and organization_id = :orgId::uuid and zone_code = :zone
        group by organization_id, organization_name, currency, zone_code
        """
    )
    suspend fun generateOrgOutstanding(orgId: String, zone: String?): List<OrgOutstanding>

    @Query(
        """
        select * from account_utilizations where document_no = :id limit 1
    """
    )
    suspend fun findByDocumentNo(id: Long?): AccountUtilization

    @Query(
        """
             select case when (amount_loc-pay_loc)=0 then 'FULL'
             when (amount_loc-pay_loc)<>0 then 'PARTIAL'
			else 'UNPAID' end as payment_status 
            from account_utilizations au where document_no =:documentNo and acc_mode =:accMode::account_mode
            """
    )
    suspend fun findDocumentStatus(documentNo: Long, accMode: String): String

    @Query(
        """
    update account_utilizations
    set document_status=:documentStatus,entity_code=:entityCode,currency=:currency,led_currency =:ledCurrency,
    amount_curr =:currAmount,amount_loc =:ledAmount,due_date =:dueDate,transaction_date =:transactionDate,
    updated_at =now() where id=:id
    """
    )
    suspend fun updateAccountUtilization(
        id: Long,
        transactionDate: Timestamp,
        dueDate: Timestamp,
        documentStatus: DocumentStatus,
        entityCode: Int,
        currency: String,
        ledCurrency: String,
        currAmount: BigDecimal,
        ledAmount: BigDecimal
    ): Int

    @Query(
        """
        UPDATE
	  account_utilizations 
        SET document_no = :documentNo, document_value = :documentValue, document_status = :documentStatus, updated_at = now()
        WHERE id = :id
    """
    )
    suspend fun updateAccountUtilization(id: Long, documentNo: Long, documentValue: String?, documentStatus: DocumentStatus): Int
}
