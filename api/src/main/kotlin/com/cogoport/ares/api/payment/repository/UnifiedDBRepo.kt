package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.InvoiceEventResponse
import com.cogoport.ares.api.common.models.OutstandingDocument
import com.cogoport.ares.api.common.models.SalesInvoiceResponse
import com.cogoport.ares.api.common.models.SalesInvoiceTimelineResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.Audit
import com.cogoport.ares.api.payment.entity.DailyOutstanding
import com.cogoport.ares.api.payment.entity.DailySalesStats
import com.cogoport.ares.api.payment.entity.KamWiseOutstanding
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.api.payment.entity.OverallAgeingStats
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import io.micronaut.transaction.annotation.TransactionalAdvice
import java.util.UUID

@TransactionalAdvice(AresConstants.UNIFIED)
@R2dbcRepository(value = AresConstants.UNIFIED, dialect = Dialect.POSTGRES)
interface UnifiedDBRepo : CoroutineCrudRepository<AccountUtilization, Long> {

    @NewSpan
    @Query(
        """
            select id, status, payment_status from plutus.invoices where created_at::varchar < :endDate and created_at::varchar > :startDate and status in ('DRAFT','FINANCE_ACCEPTED','IRN_GENERATED', 'POSTED')
        """
    )
    fun getFunnelData(startDate: String, endDate: String): List<SalesInvoiceResponse>?

    @NewSpan
    @Query(
        """
            select
                i.id as id,
                i.status as status,
                i.payment_status as payment_status,
                json_agg(json_build_object('id',ie.id,'invoice_id' , ie.invoice_id, 'event_name', ie.event_name, 'occurred_at',ie.occurred_at))::text as events
                from plutus.invoices i inner join plutus.invoice_events ie on i.id = ie.invoice_id where i.created_at::varchar < :endDate and i.created_at::varchar > :startDate
                and status in ('DRAFT','FINANCE_ACCEPTED','IRN_GENERATED', 'POSTED') and migrated = false
                group by i.id
        """
    )
    fun getInvoices(startDate: String, endDate: String): List<SalesInvoiceTimelineResponse>

    @NewSpan
    @Query(
        """
            select id,invoice_id, event_name, occurred_at  from plutus.invoice_events where invoice_id = :invoiceId
        """
    )
    fun getInvoiceEvents(invoiceId: Long): List<InvoiceEventResponse>?
    @NewSpan
    @Query(
        """
            select 
                count(*) as open_invoices_count, 
                sum(CASE when invoice_type = 'INVOICE' THEN open_invoice_amount else -1 * open_invoice_amount end) as open_invoice_amount,
                open_invoice_currency as currency, 
                count(distinct(registration_number)) as customers_count,
                shipment_service_type as service_type,
                lj.job_details  ->> 'tradeType' as trade_type,
                CASE WHEN shipment_service_type in ('fcl_freight', 'lcl_freight','fcl_customs','lcl_customs','fcl_freight_local')  THEN 'ocean'
                     WHEN shipment_service_type in ('air_customs', 'air_freight', 'domestic_air_freight')   THEN 'air'
                    WHEN shipment_service_type in ('trailer_freight', 'haulage_freight', 'trucking', 'ltl_freight', 'ftl_freight') THEN 'surface' END as grouped_services
            from temp_outstanding_invoices tod 
            inner join loki.jobs lj on lj.job_number = tod.job_number
            where registration_number is not null and open_invoice_amount > 0 and
            shipment_service_type is not null and invoice_date::varchar < :asOnDate  and  lj.job_details  ->> 'tradeType' != '' and tod.shipment_service_type !=''
            group by shipment_service_type, open_invoice_currency, lj.job_details  ->> 'tradeType' 
        """
    )
    fun getOutstandingData(asOnDate: String?): List<OutstandingDocument>?

    @NewSpan
    @Query(
        """
        select 
        to_char(date_trunc('month',transaction_date),'Mon') as duration,
        coalesce(sum(sign_flag*(amount_curr)) ,0) as amount,
        currency as dashboard_currency,
        COUNT(id) as count
        from ares.account_utilizations
        where acc_mode = 'AR' and document_status in (:docStatus)  and date_trunc('month', transaction_date) >= date_trunc('month', :asOnDate:: date - '3 month'::interval) and deleted_at is null and (:accType is null or  acc_type = :accType)
        and ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
        group by date_trunc('month',transaction_date), dashboard_currency
        """
    )
    suspend fun generateMonthlySalesStats(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            select date_trunc('day',transaction_date) as duration,
            coalesce(sum(sign_flag*(amount_curr)) ,0) as amount,
            currency as dashboard_currency,
            COUNT(id) as count
            from ares.account_utilizations
            where acc_mode = 'AR' and document_status in (:docStatus) and date_trunc('day', transaction_date) >= date_trunc('day', :asOnDate:: date - '3 day'::interval) and deleted_at is null and (:accType is null or acc_type = :accType)
            and ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            group by date_trunc('day',transaction_date), dashboard_currency
        """
    )
    suspend fun generateDailySalesStats(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
        select 
        date_trunc('year',transaction_date) as duration,
        coalesce(sum(sign_flag*(amount_curr)) ,0) as amount,
        currency as dashboard_currency,
        count(id) as count
        from ares.account_utilizations
        where 
            acc_mode = 'AR' 
            and 
            document_status in (:docStatus)  and 
            date_trunc('year', transaction_date) >= date_trunc('year', :asOnDate:: date - '3 year'::interval) 
            and deleted_at is null and 
            (:accType is null or acc_type = :accType)
            and ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
        group by date_trunc('year',transaction_date), dashboard_currency
        """
    )
    suspend fun generateYearlySalesStats(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            select date_trunc('day',lj.created_at) as duration,
            coalesce(sum((pinv.grand_total)) ,0) as amount,
            count(distinct(lj.id)) as count,
            pinv.currency as dashboard_currency
            from loki.jobs lj
            inner join plutus.invoices pinv on lj.id = pinv.job_id
            where date_trunc('day', lj.created_at) >= date_trunc('day', :asOnDate:: date - '3 day'::interval)
            group by date_trunc('day',lj.created_at),dashboard_currency
        """
    )
    suspend fun generateDailyShipmentCreatedAt(asOnDate: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            select 
            to_char(date_trunc('month',lj.created_at),'Mon') as duration,
            coalesce(sum((pinv.grand_total)) ,0) as amount,
            count(distinct(lj.id)) as count,
            pinv.currency as dashboard_currency
            from loki.jobs lj
            inner join plutus.invoices pinv on lj.id = pinv.job_id
            where date_trunc('month', lj.created_at) >= date_trunc('month', :asOnDate:: date - '3 month'::interval)
            group by date_trunc('month',lj.created_at),dashboard_currency
        """
    )
    suspend fun generateMonthlyShipmentCreatedAt(asOnDate: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            select date_trunc('year',lj.created_at) as duration,
            coalesce(sum((pinv.grand_total)) ,0) as amount,
            count(distinct(lj.id)) as count,
            pinv.currency as dashboard_currency
            from loki.jobs lj
            inner join plutus.invoices pinv on lj.id = pinv.job_id
            where date_trunc('year', lj.created_at) >= date_trunc('year', :asOnDate:: date - '3 year'::interval)
            group by date_trunc('year',lj.created_at), dashboard_currency
        """
    )
    suspend fun generateYearlyShipmentCreatedAt(asOnDate: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            select
                COALESCE(ARRAY_TO_STRING(kam_owners,', '),'Others') kam_owners,
                SUM(COALESCE(open_invoice_amount,0)) open_invoice_amount,
                (SUM(COALESCE(open_invoice_amount,0)) + SUM(COALESCE(on_account_amount,0))) total_outstanding_amount
                -- ,array_agg(distinct so.registration_number) registration_numbers
                from snapshot_organization_outstandings so
                left join (
                  with a as(
                    select
                      unnest(purm.stakeholder_rm_ids) stakeholder_rm_id, stakeholder_id, organization_id
                    from organization_stakeholders os
                    left join (select distinct user_id, array_agg(reporting_manager_id) stakeholder_rm_ids from partner_user_rm_mappings where status = 'active' group by user_id) purm on os.stakeholder_id = purm.user_id
                    where status='active'
                    AND os.stakeholder_type IN ('sales_agent', 'entity_manager')
                  ) select
                      array_agg(distinct
                        case when stakeholder_id in ('0849d0ab-5a2f-40e7-b110-971572a86192','0ccfc574-f942-4fb4-971d-a34c7ae691c3','f8347fff-f447-4adc-a9e4-fd785e16f4c2','8c22817f-4246-43ef-a7f5-fdf77e37ca72','ff4de18f-22ff-4b37-a201-8834c0caca19','b8dc5862-b7c0-4304-95e0-9d8a2b4c5c85','2eef6d5c-9ab0-4b97-8e5c-e9e8f57b8e61','7f6f97fd-c17b-4760-a09f-d70b6ad963e8','1313fb1c-7203-4010-afdd-529cd32a2308','56673bb5-872f-4750-b322-2ee98d326300','308c9961-dacb-4929-acee-89b3d9ce5163')
                        then u.name else rm_u.name end
                  ) kam_owners, organization_id
                  from a
                  inner join users u on u.id = a.stakeholder_id
                  inner join users rm_u on rm_u.id = a.stakeholder_rm_id
                  where (
                        stakeholder_id in ('0849d0ab-5a2f-40e7-b110-971572a86192','0ccfc574-f942-4fb4-971d-a34c7ae691c3','f8347fff-f447-4adc-a9e4-fd785e16f4c2','8c22817f-4246-43ef-a7f5-fdf77e37ca72','ff4de18f-22ff-4b37-a201-8834c0caca19','b8dc5862-b7c0-4304-95e0-9d8a2b4c5c85','2eef6d5c-9ab0-4b97-8e5c-e9e8f57b8e61','7f6f97fd-c17b-4760-a09f-d70b6ad963e8','1313fb1c-7203-4010-afdd-529cd32a2308','56673bb5-872f-4750-b322-2ee98d326300','308c9961-dacb-4929-acee-89b3d9ce5163')
                        or stakeholder_rm_id in ('0849d0ab-5a2f-40e7-b110-971572a86192','0ccfc574-f942-4fb4-971d-a34c7ae691c3','f8347fff-f447-4adc-a9e4-fd785e16f4c2','8c22817f-4246-43ef-a7f5-fdf77e37ca72','ff4de18f-22ff-4b37-a201-8834c0caca19','b8dc5862-b7c0-4304-95e0-9d8a2b4c5c85','2eef6d5c-9ab0-4b97-8e5c-e9e8f57b8e61','7f6f97fd-c17b-4760-a09f-d70b6ad963e8','1313fb1c-7203-4010-afdd-529cd32a2308','56673bb5-872f-4750-b322-2ee98d326300','308c9961-dacb-4929-acee-89b3d9ce5163')
                    )
                  group by organization_id
                ) os on os.organization_id = so.organization_id
                left join outstanding_account_taggings oat on oat.registration_number = so.registration_number and oat.status='active'
                where
                so.registration_number IS NOT NULL
                AND TRIM(so.registration_number) != ''
                AND is_precovid = 'NO'
                group by kam_owners
                order by total_outstanding_amount desc
                limit 10
        """
    )
    fun getKamWiseOutstanding(): List<KamWiseOutstanding>?

    @NewSpan
    @Query(
        """
            select coalesce(case when due_date >= now()::date then 'Not Due'
            when (due_days) between 1 and 30 then '1-30'
            when (due_days) between 31 and 60 then '31-60'
            when (due_days) between 61 and 90 then '61-90'
            when (due_days) between 91 and 180 then '91-180'
            when (due_days) between 181 and 365 then '181-365'
            when (due_days) > 365 then '>365' 
            end, 'Unknown') as ageing_duration, 
            sum(open_invoice_amount) as amount,
            open_invoice_currency as dashboard_currency
            from temp_outstanding_invoices toi
            inner join organizations o on o.registration_number = toi.registration_number
            inner join lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            where due_date is not null and  (:companyType is null or los.segment =:companyType)
            and (:serviceType is null or shipment_service_type = :serviceType)
            and (:cogoEntityCode is null or o.cogo_entity_id =:cogoEntityCode)
            AND ((:defaultersOrgIds) IS NULL OR o.id NOT IN (:defaultersOrgIds))
            group by ageing_duration, dashboard_currency
            order by ageing_duration
        """
    )
    fun getOutstandingByAge(serviceType: String?, defaultersOrgIds: List<UUID>?, companyType: String?, cogoEntityCode: UUID?): List<OverallAgeingStats>

    @NewSpan
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
            from ares.account_utilizations aau
            inner join organizations o on aau.organization_id = o.cogo_entity_id
            where (:serviceType is null or service_type::varchar = :serviceType) and document_status in ('FINAL', 'PROFORMA') and acc_mode = 'AR' and transaction_date <= date_trunc('month',(:date::date + '1 month'::interval)) - '1 day'::interval and deleted_at is null
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds)) 
            AND (:cogoEntityId is null or o.cogo_entity_id = :cogoEntityId)
            group by dashboard_currency
            )
            select X.month, coalesce(X.open_invoice_amount,0) as open_invoice_amount, coalesce(X.on_account_payment, 0) as on_account_payment,
            coalesce(X.outstandings, 0) as outstandings, coalesce(X.total_sales,0) as total_sales, X.days,
            coalesce((case when X.total_sales != 0 then X.outstandings / X.total_sales else 0 END) * X.days,0) as value,
            X.dashboard_currency as dashboard_currency
            from X
        """
    )
    suspend fun generateDailySalesOutstanding(date: String, serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, cogoEntityId: UUID?): MutableList<DailyOutstanding>
}
