package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.AgeingBucketZone
import com.cogoport.ares.api.payment.entity.BillOutsatndingAgeing
import com.cogoport.ares.api.payment.entity.CollectionTrend
import com.cogoport.ares.api.payment.entity.DailyOutstanding
import com.cogoport.ares.api.payment.entity.OrgOutstanding
import com.cogoport.ares.api.payment.entity.OrgStatsResponse
import com.cogoport.ares.api.payment.entity.OrgSummary
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.api.payment.entity.OutstandingAgeing
import com.cogoport.ares.api.payment.entity.OverallAgeingStats
import com.cogoport.ares.api.payment.entity.OverallStats
import com.cogoport.ares.api.payment.entity.PaymentData
import com.cogoport.ares.api.payment.model.PaymentUtilizationResponse
import com.cogoport.ares.api.settlement.entity.Document
import com.cogoport.ares.api.settlement.entity.HistoryDocument
import com.cogoport.ares.api.settlement.entity.InvoiceDocument
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.ServiceType
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.OnAccountTotalAmountResponse
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepository : CoroutineCrudRepository<AccountUtilization, Long> {
    @WithSpan
    @Query("select exists(select id from account_utilizations where document_no=:documentNo and acc_type=:accType::account_type and deleted_at is null)")
    suspend fun isDocumentNumberExists(documentNo: Long, accType: String): Boolean

    @WithSpan
    @Query(
        """select id,document_no,document_value , zone_code,service_type,document_status,entity_code , category,org_serial_id,sage_organization_id
           ,organization_id, tagged_organization_id, trade_party_mapping_id, organization_name,acc_code,acc_type,acc_mode,sign_flag,currency,led_currency,amount_curr, amount_loc,pay_curr
           ,pay_loc,due_date,transaction_date,created_at,updated_at, taxable_amount, migrated
            from account_utilizations where document_no = :documentNo and (:accType is null or acc_type= :accType::account_type) 
            and (:accMode is null or acc_mode=:accMode::account_mode) and deleted_at is null """
    )
    suspend fun findRecord(documentNo: Long, accType: String? = null, accMode: String? = null): AccountUtilization?

    @WithSpan
    @Query(
        """select id,document_no,document_value , zone_code,service_type,document_status,entity_code , category,org_serial_id,sage_organization_id
           ,organization_id, tagged_organization_id, trade_party_mapping_id,organization_name,acc_code,acc_type,acc_mode,sign_flag,currency,led_currency,amount_curr, amount_loc,pay_curr
           ,pay_loc,due_date,transaction_date,created_at,updated_at, taxable_amount, migrated
            from account_utilizations where document_value = :documentValue and (:accType is null or acc_type= :accType::account_type)
            and (:accMode is null or acc_mode=:accMode::account_mode) and deleted_at is null """
    )
    suspend fun findRecordByDocumentValue(documentValue: String, accType: String? = null, accMode: String? = null): AccountUtilization?

    @WithSpan
    @Query("delete from account_utilizations where id=:id")
    suspend fun deleteInvoiceUtils(id: Long): Int

    @WithSpan
    @Query(
        """update account_utilizations set 
              pay_curr = pay_curr + :currencyPay , pay_loc =pay_loc + :ledgerPay , updated_at =now() where id=:id and deleted_at is null"""
    )
    suspend fun updateInvoicePayment(id: Long, currencyPay: BigDecimal, ledgerPay: BigDecimal): Int

    @WithSpan
    @Query(
        """
            select coalesce(case when due_date  >= now()::date then 'Not Due'
             when (now()::date - due_date ) between 1 and 30 then '1-30'
             when (now()::date - due_date ) between 31 and 60 then '31-60'
             when (now()::date - due_date ) between 61 and 90 then '61-90'
             when (now()::date - due_date ) between 91 and 180 then '91-180'
             when (now()::date - due_date ) between 181 and 365 then '181-365'
             when (now()::date - due_date ) > 365 then '365+'
             end, 'Unknown') as ageing_duration,
             zone_code as zone,
             currency as dashboard_currency,
             sum(sign_flag * (amount_curr - pay_curr)) as amount
             from account_utilizations
             where ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
             AND (:zone is null or zone_code = :zone) and zone_code is not null and due_date is not null and acc_mode = 'AR' and acc_type in ('SINV','SCN','SDN') 
             and document_status in ('FINAL', 'PROFORMA')  and (:serviceType is null or service_type::varchar = :serviceType) and (:invoiceCurrency is null or currency = :invoiceCurrency) and deleted_at is null
             group by ageing_duration, zone, dashboard_currency
             order by 1
          """
    )
    suspend fun getReceivableByAge(zone: String?, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?): MutableList<AgeingBucketZone>

    @WithSpan
    @Query(
        """
            select coalesce(case when due_date >= now()::date then 'Not Due'
            when (now()::date - due_date) between 1 and 30 then '1-30'
            when (now()::date - due_date) between 31 and 60 then '31-60'
            when (now()::date - due_date) between 61 and 90 then '61-90'
            when (now()::date - due_date) > 90 then '>90' 
            end, 'Unknown') as ageing_duration, 
            sum(sign_flag * (amount_curr - pay_curr)) as amount,
            currency as dashboard_currency
            from account_utilizations
            where (:zone is null or zone_code = :zone) and due_date is not null and acc_mode = 'AR' and acc_type in ('SINV','SCN','SDN') and document_status in ('FINAL', 'PROFORMA') and (:serviceType is null or service_type::varchar = :serviceType) and (:invoiceCurrency is null or currency = :invoiceCurrency) and deleted_at is null
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            group by ageing_duration, dashboard_currency
            order by ageing_duration
        """

    )
    suspend fun getAgeingBucket(zone: String?, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?): List<OverallAgeingStats>

    @WithSpan
    @Query(
        """
        select
        coalesce(sum(case when acc_type in ('SINV','SDN','SCN') then sign_flag*(amount_curr - pay_curr) else 0 end),0) as open_invoices_amount,
        coalesce(sum(case when acc_type in ('SINV','SDN','SCN') and (amount_curr - pay_curr <> 0) then 1 else 0 end),0) as open_invoices_count,
        coalesce(abs(sum(case when acc_type = 'REC' and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end)),0) as open_on_account_payment_amount,
        coalesce(sum(case when acc_type in ('SINV','SDN','SCN') then sign_flag * (amount_curr - pay_curr) else 0 end) + sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end),0) as total_outstanding_amount,
        currency as dashboard_currency, 
        null as id
        from account_utilizations
        where (:zone is null or zone_code = :zone) and acc_mode = 'AR' and document_status in ('FINAL', 'PROFORMA') and (:serviceType is null or service_type::varchar = :serviceType) and (:invoiceCurrency is null or currency = :invoiceCurrency) and deleted_at is null
        AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
        group by dashboard_currency
    """
    )
    suspend fun generateOverallStats(zone: String?, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?): MutableList<OverallStats>

    @Query(
        """
        SELECT count(distinct organization_id) 
        FROM account_utilizations 
        WHERE acc_type IN ('SINV','SDN','SCN') AND amount_curr - pay_curr <> 0 AND (:zone IS NULL OR zone_code = :zone) AND document_status in ('FINAL', 'PROFORMA') AND acc_mode = 'AR' 
        AND (:serviceType IS NULL OR service_type::varchar = :serviceType) AND  (:invoiceCurrency IS NULL OR currency = :invoiceCurrency) AND deleted_at IS NULL AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
        """
    )
    suspend fun getOrganizationCountForOverallStats(zone: String?, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?): Int

    @WithSpan
    @Query(
        """
        (
            select 'Total' as duration,
            coalesce(sum(case when acc_type in ('SINV','SCN','SDN') then sign_flag*(amount_curr - pay_curr) else 0 end),0) as receivable_amount,
            coalesce(abs(sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end)),0) as collectable_amount,
            currency as dashboard_currency
            from account_utilizations
            where extract(quarter from transaction_date) = :quarter and extract(year from transaction_date) = :year and (:zone is null or zone_code = :zone) and acc_mode = 'AR' and document_status in ('FINAL', 'PROFORMA') and (:serviceType is null or service_type::varchar = :serviceType) and (:invoiceCurrency is null or currency = :invoiceCurrency) and deleted_at is null
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            group by dashboard_currency
        )
        union all
        (
            select trim(to_char(date_trunc('month',transaction_date),'Month')) as duration,
            coalesce(sum(case when acc_type in ('SINV','SCN','SDN') then sign_flag*(amount_curr - pay_curr) else 0::double precision end),0) as receivable_amount,
            coalesce(abs(sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end)),0) as collectable_amount,
            currency as dashboard_currency
            from account_utilizations
            where extract(quarter from transaction_date) = :quarter and extract(year from transaction_date) = :year and (:zone is null or zone_code = :zone) and acc_mode = 'AR' and document_status in ('FINAL', 'PROFORMA') and (:serviceType is null or service_type::varchar = :serviceType) and (:invoiceCurrency is null or currency = :invoiceCurrency) and deleted_at is null
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            group by date_trunc('month',transaction_date), dashboard_currency
            order by date_trunc('month',transaction_date)
        )
        """
    )
    suspend fun generateCollectionTrend(zone: String?, quarter: Int, year: Int, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?): MutableList<CollectionTrend>

    @WithSpan
    @Query(
        """
        with x as (
	        select to_char(generate_series(CURRENT_DATE - '4 month'::interval, CURRENT_DATE, '1 month'), 'Mon') as month
        ),
        y as (
            select to_char(date_trunc('month',transaction_date),'Mon') as month,
            sum(case when acc_type in ('SINV','SDN','SCN','SREIMB') then sign_flag*(amount_curr - pay_curr) else 0 end) + sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end) as amount,
            currency as dashboard_currency
            from account_utilizations
            where (:zone is null or zone_code = :zone) and acc_mode = 'AR' and (:serviceType is null or service_type::varchar = :serviceType) and (:invoiceCurrency is null or currency = :invoiceCurrency) and document_status in ('FINAL', 'PROFORMA') and date_trunc('month', transaction_date) >= date_trunc('month', CURRENT_DATE - '5 month'::interval) and deleted_at is null
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            group by date_trunc('month',transaction_date), dashboard_currency
        )
        select x.month duration, coalesce(y.amount, 0::double precision) as amount, 
        y.dashboard_currency
        from x left join y on y.month = x.month
        """
    )
    suspend fun generateMonthlyOutstanding(zone: String?, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?): MutableList<Outstanding>?

    @WithSpan
    @Query(
        """
            with x as (
                select extract(quarter from generate_series(CURRENT_DATE - '9 month'::interval, CURRENT_DATE, '3 month')) as quarter
            ),
            y as (
                select to_char(date_trunc('quarter',transaction_date),'Q')::int as quarter,
                sum(case when acc_type in ('SINV','SDN','SCN','SREIMB') then sign_flag*(amount_curr - pay_curr) else 0 end) + sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end) as total_outstanding_amount,
                currency as dashboard_currency
                from account_utilizations
                where acc_mode = 'AR' and (:zone is null or zone_code = :zone) and (:serviceType is null or service_type::varchar = :serviceType) and (:invoiceCurrency is null or currency = :invoiceCurrency) and document_status in ('FINAL', 'PROFORMA') and date_trunc('month', transaction_date) >= date_trunc('month',CURRENT_DATE - '9 month'::interval) and deleted_at is null
                AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
                group by date_trunc('quarter',transaction_date), dashboard_currency
            )
            select case when x.quarter = 1 then 'Jan - Mar'
            when x.quarter = 2 then 'Apr - Jun'
            when x.quarter = 3 then 'Jul - Sep'
            when x.quarter = 4 then 'Oct - Dec' end as duration,
            coalesce(y.total_outstanding_amount, 0) as amount,
            y.dashboard_currency as dashboard_currency
            from x
            left join y on x.quarter = y.quarter
        """
    )
    suspend fun generateQuarterlyOutstanding(zone: String?, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?): MutableList<Outstanding>?

    @WithSpan
    @Query(
        """
        with X as (
            select 
            extract(month from date_trunc('month',(:date)::date)) as month,
            sum(case when acc_type in ('SINV','SDN','SCN','SREIMB') then sign_flag*(amount_curr - pay_curr) else 0 end) as open_invoice_amount,
            abs(sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end)) as on_account_payment,
            sum(case when acc_type in ('SINV','SDN','SCN','SREIMB') then sign_flag*(amount_curr - pay_curr) else 0 end) + sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end) as outstandings,
            sum(case when acc_type in ('SINV','SDN','SCN','SREIMB') and transaction_date >= date_trunc('month',(:date)::date) then sign_flag*amount_curr end) as total_sales,
            case when date_trunc('month', :date::date) < date_trunc('month', now()) then date_part('days',date_trunc('month',(:date::date + '1 month'::interval)) - '1 day'::interval) 
            else date_part('days', now()::date) end as days,
            currency as dashboard_currency
            from account_utilizations
            where (:zone is null or zone_code = :zone) and (:serviceType is null or service_type::varchar = :serviceType) and (:invoiceCurrency is null or currency = :invoiceCurrency) and document_status in ('FINAL', 'PROFORMA') and acc_mode = 'AR' and transaction_date <= date_trunc('month',(:date::date + '1 month'::interval)) - '1 day'::interval and deleted_at is null
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            group by dashboard_currency
            )
            select X.month, coalesce(X.open_invoice_amount,0) as open_invoice_amount, coalesce(X.on_account_payment, 0) as on_account_payment,
            coalesce(X.outstandings, 0) as outstandings, coalesce(X.total_sales,0) as total_sales, X.days,
            coalesce((case when X.total_sales != 0 then X.outstandings / X.total_sales END) * X.days,0) as value,
            X.dashboard_currency as dashboard_currency
            from X
        """
    )
    suspend fun generateDailySalesOutstanding(zone: String?, date: String, serviceType: ServiceType?, invoiceCurrency: String?, defaultersOrgIds: List<UUID>?): MutableList<DailyOutstanding>

    @WithSpan
    @Query(
        """
        with X as (
            select 
            extract(month from date_trunc('month',(:date)::date)) as month,
            sum(case when acc_type in ('PINV','PDN','PCN','PREIMB') then sign_flag*(amount_curr - pay_curr) else 0 end) as open_invoice_amount,
            abs(sum(case when acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag*(amount_curr - pay_curr) else 0 end)) as on_account_payment,
            sum(case when acc_type in ('PINV','PDN','PCN','PREIMB') then sign_flag*(amount_curr - pay_curr) else 0 end) + sum(case when acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag*(amount_curr - pay_curr) else 0 end) as outstandings,
            sum(case when acc_type in ('PINV','PDN','PCN','PREIMB') and transaction_date >= date_trunc('month',transaction_date) then sign_flag*amount_curr end) as total_sales,
            case when date_trunc('month', :date::date) < date_trunc('month', now()) then date_part('days',date_trunc('month',(:date::date + '1 month'::interval)) - '1 day'::interval) 
            else date_part('days', now()::date) end as days,
            currency as dashboard_currency
            from account_utilizations
            where (:zone is null or zone_code = :zone) and acc_mode = 'AP' and (:serviceType is null or service_type::varchar = :serviceType) and (:invoiceCurrency is null or currency = :invoiceCurrency) and document_status in ('FINAL', 'PROFORMA') and transaction_date <= date_trunc('month',(:date::date + '1 month'::interval)) - '1 day'::interval and deleted_at is null
            group by dashboard_currency
        )
        select X.month, coalesce(X.open_invoice_amount,0) as open_invoice_amount, coalesce(X.on_account_payment, 0) as on_account_payment, coalesce(X.outstandings, 0) as outstandings, coalesce(X.total_sales,0) as total_sales, X.days,
        coalesce((X.outstandings / X.total_sales) * X.days,0) as value, dashboard_currency
        from X
        """
    )
    suspend fun generateDailyPayableOutstanding(zone: String?, date: String, serviceType: ServiceType?, invoiceCurrency: String?): MutableList<DailyOutstanding>

    @WithSpan
    @Query(
        """
        select organization_id,
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
        where organization_name ilike :queryName and (:zone is null or zone_code = :zone) and acc_mode = 'AR' 
        and due_date is not null and document_status in ('FINAL', 'PROFORMA') and organization_id is not null 
        AND ((:orgId) is NULL OR organization_id in (:orgId::uuid)) and  acc_type = 'SINV' and deleted_at is null
        AND (CASE WHEN :flag = 'defaulters' THEN organization_id IN (:defaultersOrgIds)
                 WHEN :flag = 'non_defaulters' THEN (organization_id NOT IN (:defaultersOrgIds) OR (:defaultersOrgIds) is NULL)
            END)
        and  acc_type = 'SINV'
        group by organization_id
        """
    )
    suspend fun getOutstandingAgeingBucket(zone: String?, queryName: String?, orgId: List<UUID>?, page: Int, pageLimit: Int, defaultersOrgIds: List<UUID>?, flag: String): List<OutstandingAgeing>

    @WithSpan
    @Query(
        """
        SELECT
            organization_id,
            max(organization_name) as organization_name,
            sum(
                CASE WHEN (acc_type in('PINV')
                    and(due_date >= now()::date)) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS not_due_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS today_amount,        
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) BETWEEN 1 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS thirty_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS sixty_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS ninety_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS oneeighty_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) > 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS oneeightyplus_amount,
            sum(
                CASE WHEN acc_type in('PINV') THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS total_outstanding,
            sum(
                CASE WHEN acc_type in('PCN') THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS total_credit_amount,
            sum(
                CASE WHEN due_date >= now()::date THEN
                    1
                ELSE
                    0
                END) AS not_due_count,
            sum(
                CASE WHEN (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 THEN
                    1
                ELSE
                    0
                END) AS today_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 1 AND 30 THEN
                    1
                ELSE
                    0
                END) AS thirty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 31 AND 60 THEN
                    1
                ELSE
                    0
                END) AS sixty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 61 AND 90 THEN
                    1
                ELSE
                    0
                END) AS ninety_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 THEN
                    1
                ELSE
                    0
                END) AS oneeighty_count,
            sum(
                CASE WHEN (now()::date - due_date) > 180 THEN
                    1
                ELSE
                    0
                END) AS oneeightyplus_count,
            sum(
                CASE WHEN (acc_type in('PCN')) THEN
                    1
                ELSE
                    0
                END) AS credit_note_count
        FROM
            account_utilizations
        WHERE
            (:zone IS NULL OR zone_code = :zone)
            AND acc_mode = 'AP'
            AND due_date IS NOT NULL
            AND document_status in('FINAL', 'PROFORMA')
            AND organization_id IS NOT NULL
            and(:orgId IS NULL
                OR organization_id = :orgId::uuid)
            AND acc_type in('PINV', 'PCN')
            AND deleted_at IS NULL
        GROUP BY
            organization_id
        HAVING max(organization_name) ILIKE :queryName
        OFFSET GREATEST(0, ((:page - 1) * :pageLimit))
        LIMIT :pageLimit        
        """
    )
    suspend fun getBillsOutstandingAgeingBucket(zone: String?, queryName: String?, orgId: String?, page: Int, pageLimit: Int): List<BillOutsatndingAgeing>

    @WithSpan
    @Query(
        """
            SELECT
                count(t.organization_id)
            FROM (
                SELECT
                    organization_id
                FROM
                    account_utilizations
                WHERE
                    organization_name ILIKE :queryName
                    AND (:zone IS NULL OR zone_code = :zone)
                    AND acc_mode = 'AP'
                    AND due_date IS NOT NULL
                    AND document_status in('FINAL', 'PROFORMA')
                    AND organization_id IS NOT NULL
                    AND acc_type = 'PINV'
                    AND deleted_at IS NULL
                GROUP BY
                    organization_id) AS t 
        """
    )
    suspend fun getBillsOutstandingAgeingBucketCount(zone: String?, queryName: String?, orgId: String?): Int

    @WithSpan
    @Query(
        """
        select organization_id::varchar, currency,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and amount_curr - pay_curr <> 0 then 1 else 0 end) as open_invoices_count,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_curr - pay_curr) else 0 end) as open_invoices_amount,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_loc - pay_loc) else 0 end) as open_invoices_led_amount,
        sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' and amount_curr - pay_curr <> 0 then 1 else 0 end) as payments_count,
        sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then  amount_curr - pay_curr else 0 end) as payments_amount,
        sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then  amount_loc - pay_loc else 0 end) as payments_led_amount,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_curr - pay_curr) else 0 end) + sum(case when acc_type = 'REC' and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end) as outstanding_amount,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'REC' and document_status = 'FINAL' then sign_flag*(amount_loc - pay_loc) else 0 end) as outstanding_led_amount
        from account_utilizations
        where acc_type in ('SINV','SCN','SDN','REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV', 'SREIMB') and acc_mode = 'AR' and document_status in ('FINAL', 'PROFORMA') 
        and organization_id = :orgId::uuid and (:zone is null OR zone_code = :zone) and deleted_at is null
        group by organization_id, currency
        """
    )
    suspend fun generateOrgOutstanding(orgId: String, zone: String?): List<OrgOutstanding>
    @WithSpan
    @Query(
        """
        select organization_id::varchar, currency,
        sum(case when acc_type not in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and amount_curr - pay_curr <> 0 then 1 else 0 end) as open_invoices_count,
        sum(case when acc_type not in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_curr - pay_curr) else 0 end) as open_invoices_amount,
        sum(case when acc_type not in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_loc - pay_loc) else 0 end) as open_invoices_led_amount,
        sum(case when acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' and amount_curr - pay_curr <> 0 then 1 else 0 end) as payments_count,
        sum(case when acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then  amount_curr - pay_curr else 0 end) as payments_amount,
        sum(case when acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then  amount_loc - pay_loc else 0 end) as payments_led_amount,
        sum(case when acc_type not in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_curr - pay_curr) else 0 end) + sum(case when acc_type = 'PAY' and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end) as outstanding_amount,
        sum(case when acc_type not in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'PAY' and document_status = 'FINAL' then sign_flag*(amount_loc - pay_loc) else 0 end) as outstanding_led_amount
        from account_utilizations
        where acc_type in ('PINV','SCN','SDN','PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV', 'PREIMB') and acc_mode = 'AP' and document_status in ('FINAL', 'PROFORMA') 
        and organization_id = :orgId::uuid and (:zone is null OR zone_code = :zone) and deleted_at is null
        group by organization_id, currency
        """
    )
    suspend fun generateBillOrgOutstanding(orgId: String, zone: String?): List<OrgOutstanding>

    @WithSpan
    @Query(
        value = """
        Select
            au.id,
            document_no,
            document_value,
            acc_type,
            amount_curr as amount,
            au.currency as currency,
            amount_curr-pay_curr as current_balance,
            au.led_currency,
            amount_loc as led_amount,
            taxable_amount,
            transaction_date,
            au.sign_flag,
            au.acc_mode,
            amount_loc/amount_curr as exchange_rate,
            '' as status,
            pay_curr as settled_amount,
            au.updated_at as last_edited_date,
            COALESCE(sum(case when s.source_id = au.document_no and s.source_type in ('CTDS','VTDS') then s.amount end), 0) as tds,
            COALESCE(sum(case when s.source_type in ('CTDS','VTDS') then s.amount end), 0) as settled_tds,
            COALESCE((ARRAY_AGG(s1.supporting_doc_url))[1], (ARRAY_AGG(s1.supporting_doc_url))[1]) as supporting_doc_url 
            FROM account_utilizations au
            LEFT JOIN settlements s ON
				s.destination_id = au.document_no
				AND s.destination_type::varchar = au.acc_type::varchar
            JOIN settlements s1 ON
				s1.source_id = au.document_no
				AND s1.source_type::varchar = au.acc_type::varchar
            WHERE amount_curr <> 0
                AND pay_curr <> 0
                AND organization_id in (:orgIds)
                AND acc_type::varchar in (:accountTypes)
                AND (:startDate is null or transaction_date >= :startDate::date)
                AND (:endDate is null or transaction_date <= :endDate::date)
                AND (
                    document_value ilike :query || '%' 
                    OR au.document_no in (:paymentIds)
                )
                AND s.deleted_at is null
                AND au.deleted_at is null
            GROUP BY au.id  
            ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'lastEditedDate' THEN au.updated_at
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'lastEditedDate' THEN au.updated_at
                    END        
            END 
            Asc
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    fun getHistoryDocument(
        orgIds: List<UUID>,
        accountTypes: List<String>,
        pageIndex: Int?,
        pageSize: Int?,
        startDate: String?,
        endDate: String?,
        query: String,
        paymentIds: List<Long>,
        sortBy: String?,
        sortType: String?
    ): List<HistoryDocument?>

    @WithSpan
    @Query(
        """
        SELECT count(distinct account_utilizations.id)
            FROM account_utilizations
            JOIN settlements s ON
				s.source_id = document_no
				AND s.source_type::varchar = acc_type::varchar
            WHERE
            amount_curr <> 0
            AND pay_curr <> 0
            AND organization_id in (:orgIds)
            AND acc_type::varchar in (:accountTypes)
            AND (:startDate is null or transaction_date >= :startDate::date)
            AND (:endDate is null or transaction_date <= :endDate::date)
            AND (
                    document_value ilike :query || '%' 
                    OR document_no in (:paymentIds)
                )
            AND s.deleted_at is null
            AND account_utilizations.deleted_at is null
            
        """
    )
    fun countHistoryDocument(
        orgIds: List<UUID>,
        accountTypes: List<String>,
        startDate: String?,
        endDate: String?,
        query: String,
        paymentIds: List<Long>
    ): Long

    @WithSpan
    @Query(
        """
        SELECT 
            au.id, 
            document_no, 
            document_value, 
            acc_type as account_type,
            organization_id,
            transaction_date as document_date,
            due_date, 
            document_status as invoice_status,
            COALESCE(amount_curr,0) as document_amount, 
            COALESCE(taxable_amount,0) as taxable_amount, 
            COALESCE(pay_curr,0) as settled_amount, 
            COALESCE(amount_curr - pay_curr,0) as balance_amount,
            au.currency, 
            COALESCE(sum(s.amount),0) as settled_tds
                FROM account_utilizations au
                LEFT JOIN settlements s ON 
                    s.destination_id = au.document_no 
                    AND s.destination_type::varchar = au.acc_type::varchar
                    AND s.source_type = 'CTDS'
                WHERE amount_curr <> 0 
                    AND organization_id in (:orgId)
                    AND document_status in ('FINAL', 'PROFORMA')
                    AND (:accType is null OR acc_type::varchar = :accType)
                    AND (:entityCode is null OR entity_code = :entityCode)
                    AND (:startDate is null OR transaction_date >= :startDate::date)
                    AND (:endDate is null OR transaction_date <= :endDate::date)
                    AND (
                        :status is null OR
                        CASE WHEN :status = 'PAID' then  amount_curr = pay_curr
                        WHEN :status = 'UNPAID' then  pay_curr = 0
                        WHEN :status = 'PARTIAL_PAID' then  (amount_curr - pay_curr) <> 0 AND (pay_curr > 0)
                        END
                        )
                    AND (:query is null OR document_value ilike :query)
                    AND s.deleted_at is null
                    AND au.deleted_at is null
                GROUP BY au.id
                LIMIT :limit
                OFFSET :offset
        """
    )
    suspend fun getInvoiceDocumentList(limit: Int? = null, offset: Int? = null, accType: AccountType?, orgId: List<UUID>, entityCode: Int?, startDate: Timestamp?, endDate: Timestamp?, query: String?, status: String?): List<InvoiceDocument?>

    @WithSpan
    @Query(
        """
        WITH FILTERS AS (
            SELECT id 
            FROM account_utilizations
            WHERE amount_curr <> 0 
                AND (amount_curr - pay_curr) > 0
                AND organization_id in (:orgId)
                AND document_status = 'FINAL'
                AND acc_type::varchar in (:accType)
                AND (:entityCode is null OR entity_code = :entityCode)
                AND (:startDate is null OR transaction_date >= :startDate::date)
                AND (:endDate is null OR transaction_date <= :endDate::date)
                AND document_value ilike :query
                AND (:accMode is null OR acc_mode::varchar = :accMode)
                AND deleted_at is null
            ORDER BY transaction_date DESC, id
            LIMIT :limit
            OFFSET :offset
        ), 
         MAPPINGS AS (
        	select jsonb_array_elements(account_utilization_ids)::int as id 
        	from incident_mappings
        	where incident_status = 'REQUESTED'
        	and incident_type = 'SETTLEMENT_APPROVAL'
        )
        SELECT 
            au.id,
            s.source_id,
            s.source_type,
            coalesce(s.amount,0) as settled_tds,
            s.currency as tds_currency,
            au.organization_id,
            au.trade_party_mapping_id as mapping_id,
            document_no, 
            document_value, 
            acc_type as document_type,
            acc_type as account_type,
            au.transaction_date as document_date,
            due_date, 
            COALESCE(amount_curr, 0) as document_amount, 
            COALESCE(amount_loc, 0) as document_led_amount, 
            COALESCE(amount_loc - pay_loc, 0) as document_led_balance,
            COALESCE(taxable_amount, 0) as taxable_amount,  
            COALESCE(amount_curr, 0) as after_tds_amount, 
            COALESCE(pay_curr, 0) as settled_amount, 
            COALESCE(amount_curr - pay_curr, 0) as balance_amount,
            au.currency, 
            au.led_currency, 
            au.sign_flag,
            au.acc_mode,
            CASE WHEN 
            	au.id in (select id from MAPPINGS) 
        	THEN
        		false
        	ELSE
        		true
        	END as approved,
            COALESCE(
                CASE WHEN 
                    (p.exchange_rate is not null) 
                    THEN p.exchange_rate 
                    ELSE (amount_loc / amount_curr) 
                    END,
                 1) AS exchange_rate
            FROM account_utilizations au
            LEFT JOIN payments p ON 
                p.payment_num = au.document_no
            LEFT JOIN settlements s ON 
                s.destination_id = au.document_no 
                AND s.destination_type::varchar = au.acc_type::varchar 
                AND s.source_type::varchar in ('CTDS','VTDS')
            WHERE au.id in (
                SELECT id from FILTERS
            )
            AND au.deleted_at is null
            AND s.deleted_at is null
            AND p.deleted_at is null
            ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'dueDate' THEN au.due_date
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'dueDate' THEN au.due_date
                    END        
            END 
            Asc
        """
    )
    suspend fun getDocumentList(
        limit: Int? = null,
        offset: Int? = null,
        accType: List<AccountType>,
        orgId: List<UUID>,
        entityCode: Int?,
        startDate: Timestamp?,
        endDate: Timestamp?,
        query: String?,
        accMode: AccMode?,
        sortBy: String?,
        sortType: String?
    ): List<Document?>

    @WithSpan
    @Query(
        """
        WITH FILTERS AS (
            SELECT id 
            FROM account_utilizations
            WHERE amount_curr <> 0 
                AND pay_curr <> 0
                AND organization_id in (:orgId)
                AND document_status = 'FINAL'
                AND (:accMode is null OR acc_mode = :accMode::ACCOUNT_MODE)
                AND (:accType is null OR acc_type::varchar = :accType)
                AND (:startDate is null OR transaction_date >= :startDate::date)
                AND (:endDate is null OR transaction_date <= :endDate::date)
                AND document_value ilike :query
                AND deleted_at is null
            ORDER BY transaction_date DESC, id
            LIMIT :limit
            OFFSET :offset
        ),
        MAPPINGS AS (
        	select jsonb_array_elements(account_utilization_ids)::int as id 
        	from incident_mappings
        	where incident_status = 'REQUESTED'
        	and incident_type = 'SETTLEMENT_APPROVAL'
        )
        SELECT 
            au.id,
            s.source_id,
            s.source_type,
            coalesce(s.amount,0) as settled_tds,
            s.currency as tds_currency,
            au.organization_id,
            au.trade_party_mapping_id as mapping_id,
            document_no, 
            document_value, 
            acc_type as document_type,
            acc_type as account_type,
            au.transaction_date as document_date,
            due_date, 
            COALESCE(amount_curr, 0) as document_amount, 
            COALESCE(amount_loc, 0) as document_led_amount, 
            COALESCE(taxable_amount, 0) as taxable_amount, 
            COALESCE(amount_curr, 0) as after_tds_amount, 
            COALESCE(pay_curr, 0) as settled_amount, 
            COALESCE(amount_curr - pay_curr, 0) as balance_amount,
            COALESCE(amount_loc - pay_loc, 0) as document_led_balance,
            au.currency, 
            au.led_currency, 
            au.sign_flag,
            au.acc_mode,
            CASE WHEN 
            	au.id in (select id from MAPPINGS) 
        	THEN
        		false
        	ELSE
        		true
        	END as approved,
            COALESCE(
                CASE WHEN 
                    (p.exchange_rate is not null) 
                THEN p.exchange_rate 
                ELSE (amount_loc / amount_curr) 
                END
                , 1) AS exchange_rate
            FROM account_utilizations au
            LEFT JOIN payments p ON 
                p.payment_num = au.document_no
            LEFT JOIN settlements s ON 
                s.destination_id = au.document_no 
                AND s.destination_type::varchar = au.acc_type::varchar 
                AND s.source_type::varchar in ('CTDS','VTDS')
            WHERE au.id in (
                SELECT id from FILTERS
            )
             AND au.deleted_at is null
            AND p.deleted_at is null
            AND s.deleted_at is null
            ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'dueDate' THEN au.due_date
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'dueDate' THEN au.due_date
                    END        
            END 
            Asc
        """
    )
    suspend fun getTDSDocumentList(
        limit: Int? = null,
        offset: Int? = null,
        accType: AccountType?,
        orgId: List<UUID>,
        accMode: AccMode?,
        startDate: Timestamp?,
        endDate: Timestamp?,
        query: String?,
        sortBy: String?,
        sortType: String?
    ): List<Document?>

    @WithSpan
    @Query(
        """
        SELECT 
            count(id)
                FROM account_utilizations
                WHERE 
                    amount_curr <> 0 
                    AND (amount_curr - pay_curr) > 0
                    AND document_status = 'FINAL'
                    AND organization_id in (:orgId)
                    AND acc_type::varchar in (:accType)
                    AND (:entityCode is null OR entity_code = :entityCode)
                    AND (:startDate is null OR transaction_date >= :startDate::date)
                    AND (:endDate is null OR transaction_date <= :endDate::date)
                    AND document_value ilike :query
                    AND deleted_at is null
    """
    )
    suspend fun getDocumentCount(accType: List<AccountType>, orgId: List<UUID>, entityCode: Int?, startDate: Timestamp?, endDate: Timestamp?, query: String?): Long?

    @WithSpan
    @Query(
        """
        SELECT 
            count(id)
                FROM account_utilizations
                WHERE 
                    amount_curr <> 0
                    AND document_status in ('FINAL','PROFORMA')
                    AND organization_id in (:orgId)
                    AND (:accType is null OR acc_type::varchar = :accType)
                    AND (:entityCode is null OR entity_code = :entityCode)
                    AND (:startDate is null OR transaction_date >= :startDate::date)
                    AND (:endDate is null OR transaction_date <= :endDate::date)
                    AND (
                        :status is null OR
                        CASE WHEN :status = 'PAID' then  amount_curr = pay_curr
                        WHEN :status = 'UNPAID' then  pay_curr = 0
                        WHEN :status = 'PARTIAL_PAID' then  (amount_curr - pay_curr) <> 0 AND (pay_curr > 0)
                        END
                        )
                    AND (:query is null OR document_value ilike :query)
                    AND deleted_at is null
    """
    )
    suspend fun getInvoiceDocumentCount(accType: AccountType?, orgId: List<UUID>, entityCode: Int?, startDate: Timestamp?, endDate: Timestamp?, query: String?, status: String?): Long?

    @WithSpan
    @Query(
        """
        SELECT 
            count(id)
                FROM account_utilizations
                WHERE amount_curr <> 0
                    AND pay_curr <> 0
                    AND document_status = 'FINAL'
                    AND organization_id in (:orgId)
                    AND (:accType is null OR acc_type::varchar = :accType)
                    AND (:accMode is null OR acc_mode::varchar = :accMode)
                    AND (:startDate is null OR transaction_date >= :startDate::date)
                    AND (:endDate is null OR transaction_date <= :endDate::date)
                    AND document_value ilike :query
                    AND deleted_at is null
    """
    )
    suspend fun getTDSDocumentCount(accType: AccountType?, orgId: List<UUID>, accMode: AccMode?, startDate: Timestamp?, endDate: Timestamp?, query: String?): Long?

    @WithSpan
    @Query(
        """
            SELECT coalesce(sum(sign_flag*(amount_loc-pay_loc)),0) as amount
                FROM account_utilizations
                WHERE document_status = 'FINAL'
                    AND entity_code = :entityCode
                    AND organization_id in (:orgId)
                    AND (:startDate is null or transaction_date >= :startDate)
                    AND (:endDate is null or transaction_date <= :endDate)
                    AND acc_type::varchar in (:accType)
                    AND (:accMode is null OR acc_mode::varchar = :accMode)
                    AND deleted_at is null
        """
    )
    suspend fun getAccountBalance(orgId: List<UUID>, entityCode: Int, startDate: Timestamp?, endDate: Timestamp?, accType: List<AccountType>, accMode: AccMode?): BigDecimal

    @WithSpan
    @Query(
        """
            SELECT 
                organization_id as org_id,
                organization_name as org_name,
                led_currency as currency,
                sum(
                    CASE WHEN 
                        acc_mode::varchar = :accMode AND
                        acc_type NOT IN ('REC', 'PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') 
                    THEN 
                        sign_flag * (amount_loc - pay_loc) else 0 END
                ) + 
                sum(
                    CASE WHEN 
                        acc_mode::varchar = :accMode AND
                        acc_type IN ('REC','PAY') AND document_status = 'FINAL' 
                    THEN 
                        sign_flag * (amount_loc - pay_loc) else 0 END
                ) as outstanding
                FROM account_utilizations
                WHERE organization_id = :orgId
                    AND document_status in ('FINAL', 'PROFORMA')
                    AND (:startDate is null or transaction_date >= :startDate)
                    AND (:endDate is null or transaction_date <= :endDate)
                    AND deleted_at is null
                    GROUP BY organization_id, organization_name, led_currency
        """
    )
    suspend fun getOrgSummary(orgId: UUID, accMode: AccMode, startDate: Timestamp?, endDate: Timestamp?): OrgSummary?

    @WithSpan
    @Query(
        """
        SELECT 
            :orgId as organization_id,
            MAX(ledger_currency) as ledger_currency,
            SUM(COALESCE(open_receivables,0) + COALESCE(on_account_receivables,0)) as receivables,
            SUM(COALESCE(open_payables,0) + COALESCE(on_account_payables,0)) as payables
        FROM (
            SELECT
                MAX(led_currency) as ledger_currency,
                CASE WHEN acc_type in ('SINV','SCN') then SUM(sign_flag*(amount_loc - pay_loc)) end as open_receivables,
                CASE WHEN acc_type in ('PINV','PDN','PCN') then SUM(sign_flag*(amount_loc - pay_loc)) end as open_payables,
                CASE WHEN acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then SUM(sign_flag*(amount_loc-pay_loc)) end as on_account_receivables,
                CASE WHEN acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then SUM(sign_flag*(amount_loc-pay_loc)) end as on_account_payables
            FROM account_utilizations
            WHERE 
                acc_type in ('PDN','SCN','REC','PINV','PCN','SINV','PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV')
                AND organization_id = :orgId
                AND deleted_at is null
            GROUP BY  acc_type
        ) A
    """
    )
    suspend fun getOrgStats(orgId: UUID): OrgStatsResponse?

    @WithSpan
    @Query(
        """             
            SELECT organization_id, acc_type, acc_mode, SUM(amount_curr - pay_curr) as payment_value 
            FROM account_utilizations 
            WHERE acc_type::varchar = :accType and acc_mode::varchar =:accMode and document_status != 'DELETED' and organization_id in (:organizationIdList) 
            GROUP BY organization_id, acc_type, acc_mode
        """
    )
    suspend fun onAccountPaymentAmount(accType: AccountType, accMode: AccMode, organizationIdList: List<UUID>): MutableList<OnAccountTotalAmountResponse>

    @WithSpan
    @Query(
        """
        SELECT 
            document_no,
            transaction_date::timestamp AS transaction_date, 
            amount_loc/amount_curr as exchange_rate
        FROM account_utilizations
        WHERE acc_type::varchar in (:documentType) 
        AND document_no in (:documentNo)
        AND deleted_at is null
    """
    )
    suspend fun getPaymentDetails(documentNo: List<Long>, documentType: List<SettlementType>): List<PaymentData>

    @WithSpan
    @Query(
        """
                SELECT
                id,
                document_no,
                document_value,
                zone_code,
                service_type,
                document_status,
                entity_code,
                category,
                org_serial_id,
                sage_organization_id,
                organization_id,
                organization_name,
                acc_code,
                acc_type,
                acc_mode,
                sign_flag,
                currency,
                led_currency,
                amount_curr,
                amount_loc,
                pay_curr,
                pay_loc,
                due_date,
                transaction_date,
                created_at,
                updated_at,
                taxable_amount,
                trade_party_mapping_id,
                tagged_organization_id,
                migrated
                FROM account_utilizations
                WHERE document_value = :documentValue
                AND   acc_type = :accType::account_type
                AND deleted_at is null
            """
    )
    suspend fun getAccountUtilizationsByDocValue(documentValue: String, accType: AccountType?): AccountUtilization

    @WithSpan
    @Query(
        """
                SELECT
                id,
                document_no,
                document_value,
                zone_code,
                service_type,
                document_status,
                entity_code,
                category,
                org_serial_id,
                sage_organization_id,
                organization_id,
                organization_name,
                acc_code,
                acc_type,
                acc_mode,
                sign_flag,
                currency,
                led_currency,
                amount_curr,
                amount_loc,
                pay_curr,
                pay_loc,
                due_date,
                transaction_date,
                created_at,
                updated_at,
                taxable_amount,
                trade_party_mapping_id,
                tagged_organization_id,
                migrated
                FROM account_utilizations
                WHERE document_no = :documentNo
                AND   acc_type = :accType::account_type
                AND deleted_at is null
            """
    )
    suspend fun getAccountUtilizationsByDocNo(documentNo: String, accType: AccountType): AccountUtilization

    @WithSpan
    @Query(
        """ 
        SELECT
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_proforma_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN 1 ELSE 0 END),0) as proforma_invoices_count,
        (SELECT count(distinct tagged_organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') AND amount_loc - pay_loc <> 0 AND document_value in (:documentValues) AND document_status = 'PROFORMA' AND deleted_at is null) as customers_count_proforma,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date > now()::date AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_due_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND (amount_loc- pay_loc <> 0) AND due_date > now()::date AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as due_invoices_count,
        (SELECT count(distinct tagged_organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') AND amount_loc - pay_loc <> 0 AND document_value in (:documentValues) AND document_status in ('FINAL','PROFORMA') AND due_date > now()::date AND deleted_at is null) as customers_count_due,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date AND (amount_loc- pay_loc <> 0) AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as overdue_invoices_count,
        (SELECT count(distinct tagged_organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') AND amount_curr - pay_curr <> 0 AND document_value in (:documentValues) AND document_status in ('FINAL','PROFORMA') AND due_date <= now()::date AND deleted_at is null) as customers_count_overdue,
        COALESCE(sum(case when acc_type in ('SINV','SCN','SDN') AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_amount_receivables,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status in ('FINAL','PROFORMA') AND (amount_loc- pay_loc <> 0) AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as receivables_invoices_count,
        (SELECT count(distinct tagged_organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') AND amount_loc - pay_loc <> 0 AND document_value in (:documentValues) AND document_status in ('FINAL','PROFORMA') AND deleted_at is null) as customers_count_receivables,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 AND due_date is NOT NULL THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60 AND due_date is NOT NULL THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 AND due_date is NOT NULL THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(sum(case when (now()::date - due_date) >90 AND due_date is NOT NULL THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) AND due_date is NOT NULL THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) AND due_date is NOT NULL THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) AND due_date is NOT NULL THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(sum(case when (now()::date - due_date) >90 AND (amount_loc - pay_loc <> 0) AND due_date is NOT NULL THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value in (:documentValues) AND tagged_organization_id IS NOT NULL AND deleted_at is null
        """
    )
    suspend fun getOverallStats(documentValues: List<String>): StatsForKamResponse

    @WithSpan
    @Query(
        """ 
        SELECT * from
        (SELECT tagged_organization_id as organization_id,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_proforma_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN 1 ELSE 0 END),0) as proforma_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date > now()::date AND  document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_due_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND (amount_loc - pay_loc <> 0) AND due_date > now()::date AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as due_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date AND (amount_loc- pay_loc <> 0) AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as overdue_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SCN','SDN') AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_amount_receivables,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status in ('FINAL','PROFORMA') AND (amount_loc- pay_loc <> 0) AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as receivables_invoices_count,
        COALESCE(abs(sum(case when acc_type = 'REC' AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END)),0) as on_account_payment,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60  THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(sum(case when (now()::date - due_date) > 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(sum(case when (now()::date - due_date) > 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value in (:documentValues) AND due_date IS NOT NULL AND  amount_curr <> 0 AND tagged_organization_id IS NOT NULL 
        AND (:bookingPartyId is NULL OR tagged_organization_id = :bookingPartyId::uuid) AND deleted_at is null
        GROUP BY tagged_organization_id) output
        ORDER BY
            CASE WHEN :sortBy = 'Desc' THEN
                    CASE WHEN :sortType = 'proforma_invoices_count' THEN output.proforma_invoices_count
                         WHEN :sortType = 'overdue_invoices_count' THEN output.overdue_invoices_count
                         WHEN :sortType = 'due_invoices_count' THEN output.due_invoices_count
                    END
            END 
            Desc,
            CASE WHEN :sortBy = 'Asc' THEN
                    CASE WHEN :sortType = 'proforma_invoices_count' THEN output.proforma_invoices_count
                         WHEN :sortType = 'overdue_invoices_count' THEN output.overdue_invoices_count
                         WHEN :sortType = 'due_invoices_count' THEN output.due_invoices_count 
                    END        
            END 
            Asc,
            CASE WHEN :sortType is NULL and :sortBy is NULL THEN output.due_invoices_count END Desc
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    suspend fun getOverallStatsForCustomers(
        documentValues: List<String>,
        bookingPartyId: String?,
        pageIndex: Int,
        pageSize: Int,
        sortType: String?,
        sortBy: String?
    ): List<StatsForCustomerResponse?>

    @WithSpan
    @Query(
        """ 
        SELECT count(*) from
        (SELECT tagged_organization_id as organization_id,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_proforma_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN 1 ELSE 0 END),0) as proforma_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date > now()::date AND  document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_due_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND (amount_loc - pay_loc <> 0) AND due_date > now()::date AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as due_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date AND (amount_loc- pay_loc <> 0) AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as overdue_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SCN','SDN') AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_amount_receivables,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status in ('FINAL','PROFORMA') AND (amount_loc- pay_loc <> 0) AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as receivables_invoices_count,
        COALESCE(abs(sum(case when acc_type = 'REC' AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END)),0) as on_account_payment,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60  THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(sum(case when (now()::date - due_date) > 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(sum(case when (now()::date - due_date) > 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value in (:documentValues) AND due_date IS NOT NULL AND  amount_curr <> 0 AND tagged_organization_id IS NOT NULL 
        AND (:bookingPartyId is NULL OR tagged_organization_id = :bookingPartyId::uuid) AND deleted_at is null
        GROUP BY tagged_organization_id) as output
        """
    )
    suspend fun getCount(
        documentValues: List<String>,
        bookingPartyId: String?
    ): Long?

    @WithSpan
    @Query(
        """
            DELETE FROM account_utilizations WHERE document_value IN (:docValues) and acc_type IN ('SINV', 'SDN') and acc_mode='AR' and document_status='PROFORMA'
        """
    )
    suspend fun deleteConsolidatedInvoices(docValues: List<String>)

    @WithSpan
    @Query("select current_date - min(due_date) from account_utilizations au where amount_loc - pay_loc != 0 and acc_type = 'SINV' and document_no in (:invoiceIds) AND deleted_at is null")
    suspend fun getCurrentOutstandingDays(invoiceIds: List<Long>): Long

    @WithSpan
    @Query(
        """
            UPDATE account_utilizations SET deleted_at = NOW() WHERE id = :id
        """
    )
    suspend fun deleteAccountUtilization(id: Long)

    @WithSpan
    @Query(
        """UPDATE account_utilizations SET 
              pay_curr = :currencyPay , pay_loc = :ledgerPay , updated_at = NOW() WHERE id =:id AND deleted_at is null"""
    )
    suspend fun updateAccountUtilization(id: Long, currencyPay: BigDecimal, ledgerPay: BigDecimal)

    @WithSpan
    @Query(
        """
            SELECT id,pay_curr,pay_loc FROM account_utilizations WHERE document_no = :paymentNum AND acc_mode = 'AP' AND deleted_at is null
        """
    )
    suspend fun getDataByPaymentNum(paymentNum: Long?): PaymentUtilizationResponse

    @WithSpan
    @Query(
        """
           SELECT DISTINCT (au.document_no),au.id,au.document_value, au.zone_code,au.service_type,
           au.document_status,au.entity_code, au.category,au.org_serial_id,au.sage_organization_id,
           au.organization_id,au.tagged_organization_id,au.trade_party_mapping_id, au.organization_name,
           au.acc_code,au.acc_type,au.acc_mode,au.sign_flag,au.currency,au.led_currency,au.amount_curr,
           au.amount_loc,au.pay_curr,au.pay_loc,au.due_date,au.transaction_date,au.updated_at, au.taxable_amount,
           au.migrated,au.created_at         
           FROM 
           payments p
           JOIN payment_invoice_mapping pim ON 
           pim.payment_id = p.id AND pim.document_no = :documentNo
           JOIN account_utilizations au ON 
           au.document_no = p.payment_num AND au.deleted_at is null AND au.acc_mode = 'AP'
           ORDER by au.created_at desc
        """
    )
    suspend fun findPaymentsByDocumentNo(documentNo: Long): List<AccountUtilization?>

    @WithSpan
    @Query(
        """ 
        SELECT organization_id,
        COALESCE(SUM(CASE WHEN acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(SUM(CASE WHEN acc_type in ('SINV','SDN','SCN') then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_loc - pay_loc) else 0 end),0) as total_outstanding_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 0 AND 30 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 31 AND 60  THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 61 AND 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) > 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) > 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value IN (:documentValues) AND due_date IS NOT NULL AND  amount_curr <> 0 AND organization_id IS NOT NULL 
        AND deleted_at IS NULL
        GROUP BY organization_id
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    suspend fun getOverallStatsForTradeParty(
        documentValues: List<String>,
        pageIndex: Int,
        pageSize: Int
    ): List<OverallStatsForTradeParty?>

    @WithSpan
    @Query(
        """ 
        SELECT COUNT(*) FROM
        (SELECT organization_id,
        COALESCE(SUM(CASE WHEN acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(SUM(CASE WHEN acc_type in ('SINV','SDN','SCN') then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_loc - pay_loc) else 0 end),0) as total_outstanding_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 0 AND 30 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 31 AND 60  THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 61 AND 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) > 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) > 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value IN (:documentValues) AND due_date IS NOT NULL AND  amount_curr <> 0 AND organization_id IS NOT NULL 
        AND deleted_at IS NULL
        GROUP BY organization_id) as output
        """
    )
    suspend fun getTradePartyCount(
        documentValues: List<String>
    ): Long?

    @WithSpan
    @Query(
        """ 
         SELECT * FROM
        (SELECT organization_id::VARCHAR,
        document_value as document_number ,
        document_status AS document_type,
        service_type,
        amount_loc AS invoice_amount,
        sign_flag*(amount_loc - pay_loc) as outstanding_amount
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value IN (:documentValues) AND organization_id IS NOT NULL 
        AND deleted_at IS NULL) output
        ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                    CASE WHEN :sortBy = 'invoice_amount' THEN output.invoice_amount
                         WHEN :sortBy = 'outstanding_amount' THEN output.outstanding_amount
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'invoice_amount' THEN output.invoice_amount
                         WHEN :sortBy = 'outstanding_amount' THEN output.outstanding_amount 
                    END        
            END 
            Asc,
            CASE WHEN :sortType is NULL and :sortBy is NULL THEN output.invoice_amount END Desc
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    suspend fun getInvoiceListForTradeParty(
        documentValues: List<String>,
        sortBy: String?,
        sortType: String?,
        pageIndex: Int,
        pageSize: Int
    ): List<InvoiceListResponse>

    @WithSpan
    @Query(
        """ 
         SELECT COUNT(*) FROM
        (SELECT organization_id::VARCHAR,
        document_value as document_number ,
        document_status AS document_type,
        service_type,
        amount_loc AS invoice_amount,
        sign_flag*(amount_loc - pay_loc) as outstanding_amount
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value IN (:documentValues) AND organization_id IS NOT NULL 
        AND deleted_at IS NULL) as output
        """
    )
    suspend fun getInvoicesCountForTradeParty(
        documentValues: List<String>
    ): Long?
}
