package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.OutstandingDocument
import com.cogoport.ares.api.common.models.SalesInvoiceResponse
import com.cogoport.ares.api.common.models.SalesInvoiceTimelineResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@TransactionalAdvice(AresConstants.UNIFIED)
@R2dbcRepository(value = AresConstants.UNIFIED, dialect = Dialect.POSTGRES)
interface UnifiedDBRepo : CoroutineCrudRepository<AccountUtilization, Long> {

    @NewSpan
    @Query(
        """
            SELECT 
            distinct (sinv.id) as id, 
            sinv.status, 
            sinv.payment_status
            FROM 
            ares.account_utilizations aau 
            INNER JOIN plutus.invoices sinv on sinv.id = aau.document_no
            INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id
            INNER JOIN loki.jobs lj on lj.id = sinv.job_id
            LEFT JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on o.lead_organization_id = los.lead_organization_id
            WHERE 
            EXTRACT(YEAR FROM aau.transaction_date) = :year
            AND EXTRACT(MONTH FROM aau.transaction_date) = :month
            AND sinv.status in ('DRAFT','FINANCE_ACCEPTED','IRN_GENERATED', 'POSTED') 
            AND (aau.entity_code = :entityCode)
            AND aau.acc_type in ('SINV', 'SCN')
            AND o.status = 'active'
            AND (aau.migrated = false)
            AND (sinv.migrated = false)
            AND (pa.organization_type = 'BUYER')
            AND (:companyType is null OR los.id is null OR los.segment = :companyType )
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
        """
    )
    fun getFunnelData(entityCode: Int?, companyType: String?, serviceType: String?, year: Int?, month: Int?): List<SalesInvoiceResponse>?

    @NewSpan
    @Query(
        """
            SELECT 
            distinct(aau.document_no) as id, 
            sinv.status, 
            sinv.payment_status,
            json_agg(
                json_build_object(
                'id',ie.id,
                'invoice_id' , ie.invoice_id, 
                'event_name', ie.event_name, 
                'occurred_at',ie.occurred_at
                )
            )::text as events
            FROM 
            ares.account_utilizations aau 
            INNER JOIN plutus.invoices sinv on sinv.id = aau.document_no
            INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id
            INNER JOIN plutus.invoice_events ie on ie.invoice_id = sinv.id
            INNER JOIN loki.jobs lj on lj.id = sinv.job_id
            LEFT JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on o.lead_organization_id = los.lead_organization_id
            WHERE 
            EXTRACT(YEAR FROM aau.transaction_date) = :year
            AND EXTRACT(MONTH FROM aau.transaction_date) = :month
            AND sinv.status in ('DRAFT','FINANCE_ACCEPTED','IRN_GENERATED', 'POSTED') 
            AND (aau.entity_code = :entityCode)
            AND aau.acc_type in ('SINV', 'SCN')
            AND (aau.migrated = false)
            AND (sinv.migrated = false)
            AND (pa.organization_type = 'BUYER')
            AND (:companyType is null OR los.id is null OR los.segment = :companyType )
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            group by aau.document_no, sinv.status, sinv.payment_status
        """
    )
    fun getInvoices(year: Int?, month: Int?, entityCode: Int?, companyType: String?, serviceType: String?): List<SalesInvoiceTimelineResponse>
    @NewSpan
    @Query(
        """
        SELECT coalesce(sum((amount_loc-pay_loc)),0) as amount
        FROM ares.account_utilizations aau
        WHERE document_status = 'FINAL'
        AND (:entityCode is null OR aau.entity_code = :entityCode)
        AND aau.transaction_date < NOW() 
        AND acc_type = 'REC'
        AND (acc_mode = 'AR')
        AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
        AND deleted_at is null
        """
    )
    fun getOnAccountAmount(entityCode: Int?, defaultersOrgIds: List<UUID>? = null): BigDecimal?

    @NewSpan
    @Query(
        """
            SELECT  
            COUNT(distinct (aau.organization_id)) as customers_count,
            COUNT(aau.document_no) as open_invoices_count,
            SUM(aau.sign_flag*(aau.amount_loc-aau.pay_loc)) as open_invoice_amount,
            aau.led_currency as currency,
            aau.service_type,
            lj.job_details  ->> 'tradeType' as trade_type,
            CASE WHEN aau.service_type in ('FCL_FREIGHT', 'LCL_FREIGHT','FCL_CUSTOMS','LCL_CUSTOMS','FCL_FREIGHT_LOCAL')  THEN 'ocean'
                 WHEN aau.service_type in ('AIR_CUSTOMS', 'AIR_FREIGHT', 'DOMESTIC_AIR_FREIGHT')   THEN 'air'
                 WHEN aau.service_type in ('TRAILER', 'HAULAGE_FREIGHT', 'TRUCKING', 'LTL_FREIGHT', 'FTL_FREIGHT', 'RAIL_DOMESTIC_FREIGHT') THEN 'surface'
                 ELSE 'others'
            END as grouped_services
            FROM 
            ares.account_utilizations aau 
            INNER JOIN plutus.invoices pinv on pinv.id = aau.document_no
            INNER JOIN loki.jobs lj on lj.id = pinv.job_id
            where
            aau.trade_party_mapping_id is not null 
            AND acc_mode ='AR'
            AND acc_type in ('SINV', 'SCN')
            AND aau.migrated = false
            AND (amount_loc-pay_loc) > 0 
            AND aau.transaction_date::date <= Now()
            AND (:entityCode is null or aau.entity_code = :entityCode)
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            group by aau.led_currency,aau.service_type, aau.led_currency, lj.job_details  ->> 'tradeType'
        """
    )
    fun getOutstandingData(entityCode: Int?, defaultersOrgIds: List<UUID>? = null): List<OutstandingDocument>?

    @NewSpan
    @Query(
        """
            SELECT coalesce(sum((amount_loc-pay_loc)),0) as amount
            FROM ares.account_utilizations aau
            WHERE document_status = 'FINAL'
            AND (:entityCode is null OR aau.entity_code = :entityCode)
            AND aau.transaction_date < NOW() 
            AND acc_type = 'REC'
            AND (acc_mode = 'AR')
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            AND deleted_at is null
            AND date_trunc('day', aau.transaction_date) > date_trunc('day', NOW():: date - '7 day'::interval)
        """
    )
    fun getOnAccountAmountForPastSevenDays(entityCode: Int?, defaultersOrgIds: List<UUID>? = null): BigDecimal?

    @Query(
        """
            SELECT  
            SUM(sign_flag*(amount_loc-pay_loc)) as amount,
            led_currency as currency
            FROM 
            ares.account_utilizations aau 
            WHERE
            date_trunc('day', aau.transaction_date) > date_trunc('day', NOW():: date - '7 day'::interval)
            AND aau.acc_mode ='AR'
            AND acc_type in ('SINV','SCN')
            AND (aau.entity_code = :entityCode)
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            AND (amount_loc-pay_loc) > 0
            GROUP BY led_currency
        """
    )

    fun getOutstandingAmountForPastSevenDays(entityCode: Int?, defaultersOrgIds: List<UUID>? = null): BigDecimal?

    @NewSpan
    @Query(
        """
        with x as (
            SELECT 
            distinct(aau.document_no) as id,
            date_trunc('month',aau.transaction_date) as duration,
            aau.amount_loc as amount,
            aau.led_currency as dashboard_currency
            from ares.account_utilizations aau
            INNER JOIN organizations o on o.id = aau.tagged_organization_id
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE 
            aau.acc_mode = 'AR' 
            AND 
            aau.document_status = 'FINAL'
            AND aau.transaction_date > :quarterStart::DATE
            AND aau.transaction_date < :quarterEnd::DATE
            AND aau.deleted_at is null 
            AND (aau.acc_type::VARCHAR = :accType)
            AND ((:defaultersOrgIds) IS NULL OR aau.organization_id NOT IN (:defaultersOrgIds))
            AND ( aau.entity_code = :entityCode)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:serviceType is null or aau.service_type = :serviceType) 
            ) SELECT x.duration, sum(x.amount) as amount, count(x.id) as count, x.dashboard_currency from x
            GROUP BY x.duration, x.dashboard_currency
            ORDER BY x.duration DESC
        """
    )
    suspend fun generateMonthlySalesStats(quarterStart: LocalDateTime, quarterEnd: LocalDateTime, accType: String, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: String?, serviceType: ServiceType?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            with x as (
            SELECT 
            distinct(aau.document_no) as id,
            date_trunc('day',aau.transaction_date) as duration,
            aau.amount_loc as amount,
            aau.led_currency as dashboard_currency
            from ares.account_utilizations aau
            INNER JOIN organizations o on o.id = aau.tagged_organization_id
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE 
            aau.acc_mode = 'AR' 
            AND 
            aau.document_status = 'FINAL'
            AND date_trunc('day', aau.transaction_date) >= date_trunc('day', :asOnDate:: date - '3 day'::interval)
            AND date_trunc('day', aau.transaction_date) < date_trunc('day', :asOnDate:: date + '1 day'::interval)
            AND aau.deleted_at is null 
            AND (aau.acc_type::VARCHAR = :accType)
            AND ((:defaultersOrgIds) IS NULL OR aau.organization_id NOT IN (:defaultersOrgIds))
            AND ( aau.entity_code = :entityCode)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:serviceType is null or aau.service_type = :serviceType) 
            ) SELECT x.duration, sum(x.amount) as amount, count(x.id) as count, x.dashboard_currency from x
            GROUP BY x.duration, x.dashboard_currency
            ORDER BY x.duration DESC
        """
    )
    suspend fun generateDailySalesStats(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: String?, serviceType: ServiceType?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
        with x as (
        SELECT 
        distinct(aau.document_no) as id,
        date_trunc('year',aau.transaction_date) as duration,
        aau.amount_loc as amount,
        aau.led_currency as dashboard_currency
        from ares.account_utilizations aau
        INNER JOIN organizations o on o.id = aau.tagged_organization_id
        LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
        WHERE 
        aau.acc_mode = 'AR' 
        AND 
        aau.document_status = 'FINAL'
        AND date_trunc('year', aau.transaction_date) >= date_trunc('year', :asOnDate:: date - '3 year'::interval)
        AND date_trunc('year', aau.transaction_date) < date_trunc('year', :asOnDate:: date + '1 year'::interval)
        AND aau.deleted_at is null 
        AND (aau.acc_type = :accType)
        AND ((:defaultersOrgIds) IS NULL OR aau.organization_id NOT IN (:defaultersOrgIds))
        AND ( aau.entity_code = :entityCode)
        AND (:companyType is null or los.segment = :companyType OR los.id is null)
        AND (:serviceType is null or aau.service_type = :serviceType) 
        ) SELECT x.duration, sum(x.amount) as amount, count(x.id) as count, x.dashboard_currency from x
        GROUP BY x.duration, x.dashboard_currency
        ORDER BY x.duration DESC
        """
    )
    suspend fun generateYearlySalesStats(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: String?, serviceType: ServiceType?): MutableList<DailySalesStats>?
    @NewSpan
    @Query(
        """
            SELECT 
            date_trunc('day',lj.created_at) as duration,
            coalesce(sum(CASE when invoice_type = 'INVOICE' THEN pinv.ledger_total else -1 * (pinv.ledger_total) end), 0) as amount,
            count(distinct(lj.id)) as count,
            pinv.ledger_currency as dashboard_currency
            FROM loki.jobs lj
            INNER JOIN plutus.invoices pinv on lj.id = pinv.job_id
            INNER JOIN plutus.addresses pa on pa.invoice_id = pinv.id
            INNER JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE date_trunc('day', lj.created_at) >= date_trunc('day', :asOnDate:: date - '3 day'::interval)
            AND date_trunc('day', lj.created_at) <= date_trunc('day', :asOnDate:: date)
            AND (:companyType is null OR los.id is null OR los.segment = :companyType)
            AND (pa.entity_code = :entityCode)
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            AND (pinv.status not in ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND (pa.organization_type = 'BUYER')
            AND o.status = 'active'
            GROUP BY date_trunc('day',lj.created_at), dashboard_currency
        """
    )
    suspend fun generateDailyShipmentCreatedAt(asOnDate: String?, entityCode: Int?, companyType: String?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            SELECT 
            date_trunc('month',lj.created_at) as duration,
            coalesce(sum(CASE when invoice_type = 'INVOICE' THEN pinv.ledger_total else -1 * (pinv.ledger_total) end), 0) as amount,
            count(distinct(lj.id)) as count,
            pinv.ledger_currency as dashboard_currency
            FROM loki.jobs lj
            INNER JOIN plutus.invoices pinv on lj.id = pinv.job_id
            INNER JOIN plutus.addresses pa on pa.invoice_id = pinv.id
            INNER JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE date_trunc('month', lj.created_at) >= date_trunc('month', :asOnDate:: date - '3 month'::interval)
            AND date_trunc('month', lj.created_at) <= date_trunc('month', :asOnDate:: date)
            AND (:companyType is null OR  los.id is null OR los.segment = :companyType)
            AND (pa.entity_code = :entityCode)
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            AND (pinv.status not in ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND (pa.organization_type = 'BUYER')
            AND o.status = 'active'
            GROUP BY date_trunc('month',lj.created_at), dashboard_currency
        """
    )
    suspend fun generateMonthlyShipmentCreatedAt(asOnDate: LocalDateTime?, entityCode: Int?, companyType: String?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            SELECT 
            date_trunc('year',lj.created_at) as duration,
            coalesce(sum(CASE when invoice_type = 'INVOICE' THEN pinv.ledger_total else -1 * (pinv.ledger_total) end), 0) as amount,
            count(distinct(lj.id)) as count,
            pinv.ledger_currency as dashboard_currency
            FROM loki.jobs lj
            INNER JOIN plutus.invoices pinv on lj.id = pinv.job_id
            INNER JOIN plutus.addresses pa on pa.invoice_id = pinv.id
            INNER JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE date_trunc('year', lj.created_at) >= date_trunc('year', :asOnDate:: date - '3 year'::interval)
            AND date_trunc('year', lj.created_at) <= date_trunc('year', :asOnDate:: date)
            AND (:companyType is null OR los.id is null OR los.segment = :companyType )
            AND ( pa.entity_code = :entityCode)
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            AND (pinv.status not in ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND (pa.organization_type = 'BUYER')
            AND o.status = 'active'
            GROUP BY date_trunc('year' , lj.created_at), dashboard_currency
        """
    )
    suspend fun generateYearlyShipmentCreatedAt(asOnDate: String?, entityCode: Int?, companyType: String?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            SELECT
                COALESCE(ARRAY_TO_STRING(kam_owners,', '),'Others') kam_owners,
                SUM(COALESCE(open_invoice_amount,0)) open_invoice_amount,
                (SUM(COALESCE(open_invoice_amount,0)) + SUM(COALESCE(on_account_amount,0))) total_outstanding_amount
                -- ,array_agg(distinct so.registration_number) registration_numbers
                from snapshot_organization_outstandings so
                LEFT JOIN (
                  with a as(
                    SELECT
                      unnest(purm.stakeholder_rm_ids) stakeholder_rm_id, stakeholder_id, organization_id
                    from organization_stakeholders os
                    LEFT JOIN (select distinct user_id, array_agg(reporting_manager_id) stakeholder_rm_ids from partner_user_rm_mappings where status = 'active' group by user_id) purm on os.stakeholder_id = purm.user_id
                    WHERE status='active'
                    AND os.stakeholder_type IN ('sales_agent', 'entity_manager')
                  ) SELECT
                      array_agg(distinct
                        case when stakeholder_id in ('0849d0ab-5a2f-40e7-b110-971572a86192','0ccfc574-f942-4fb4-971d-a34c7ae691c3','f8347fff-f447-4adc-a9e4-fd785e16f4c2','8c22817f-4246-43ef-a7f5-fdf77e37ca72','ff4de18f-22ff-4b37-a201-8834c0caca19','b8dc5862-b7c0-4304-95e0-9d8a2b4c5c85','2eef6d5c-9ab0-4b97-8e5c-e9e8f57b8e61','7f6f97fd-c17b-4760-a09f-d70b6ad963e8','1313fb1c-7203-4010-afdd-529cd32a2308','56673bb5-872f-4750-b322-2ee98d326300','308c9961-dacb-4929-acee-89b3d9ce5163')
                        then u.name else rm_u.name end
                  ) kam_owners, organization_id
                  from a
                  INNER JOIN users u on u.id = a.stakeholder_id
                  INNER JOIN users rm_u on rm_u.id = a.stakeholder_rm_id
                  WHERE (
                        stakeholder_id in ('0849d0ab-5a2f-40e7-b110-971572a86192','0ccfc574-f942-4fb4-971d-a34c7ae691c3','f8347fff-f447-4adc-a9e4-fd785e16f4c2','8c22817f-4246-43ef-a7f5-fdf77e37ca72','ff4de18f-22ff-4b37-a201-8834c0caca19','b8dc5862-b7c0-4304-95e0-9d8a2b4c5c85','2eef6d5c-9ab0-4b97-8e5c-e9e8f57b8e61','7f6f97fd-c17b-4760-a09f-d70b6ad963e8','1313fb1c-7203-4010-afdd-529cd32a2308','56673bb5-872f-4750-b322-2ee98d326300','308c9961-dacb-4929-acee-89b3d9ce5163')
                        or stakeholder_rm_id in ('0849d0ab-5a2f-40e7-b110-971572a86192','0ccfc574-f942-4fb4-971d-a34c7ae691c3','f8347fff-f447-4adc-a9e4-fd785e16f4c2','8c22817f-4246-43ef-a7f5-fdf77e37ca72','ff4de18f-22ff-4b37-a201-8834c0caca19','b8dc5862-b7c0-4304-95e0-9d8a2b4c5c85','2eef6d5c-9ab0-4b97-8e5c-e9e8f57b8e61','7f6f97fd-c17b-4760-a09f-d70b6ad963e8','1313fb1c-7203-4010-afdd-529cd32a2308','56673bb5-872f-4750-b322-2ee98d326300','308c9961-dacb-4929-acee-89b3d9ce5163')
                    )
                  GROUP BY organization_id
                ) os on os.organization_id = so.organization_id
                LEFT JOIN outstanding_account_taggings oat on oat.registration_number = so.registration_number and oat.status='active'
                WHERE
                so.registration_number IS NOT NULL
                AND TRIM(so.registration_number) != ''
                AND is_precovid = 'NO'
                GROUP BY kam_owners
                order by total_outstanding_amount desc
                limit 10
        """
    )
    fun getKamWiseOutstanding(): List<KamWiseOutstanding>?

    @NewSpan
    @Query(
        """
            select coalesce(
                case 
                WHEN due_date >= now()::date then 'Not Due'
                WHEN (now()::date - due_date) between 1 AND 30 then '1-30'
                WHEN (now()::date - due_date) between 31 AND 60 then '31-60'
                WHEN (now()::date - due_date) between 61 AND 90 then '61-90'
                WHEN (now()::date - due_date) between 91 AND 180 then '91-180'
                WHEN (now()::date - due_date) between 181 AND 365 then '181-365'
                WHEN (now()::date - due_date) > 365 then '>365' 
                end
            ) as ageing_duration, 
            sum(sign_flag * (amount_loc- pay_loc)) as amount,
            led_currency as dashboard_currency
            from ares.account_utilizations aau
            INNER JOIN organization_trade_parties otp on otp.id = aau.trade_party_mapping_id
            INNER JOIN organizations o on o.id = otp.organization_id
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE 
            due_date is not null 
            AND acc_mode = 'AR' 
            AND 
            acc_type in ('SINV','SDN') 
            AND document_status in ('FINAL') 
            AND deleted_at is null
            AND ((:defaultersOrgIds) IS NULL OR aau.organization_id NOT IN (:defaultersOrgIds))
            AND (:companyType is null OR los.id is null OR los.segment =:companyType)
            AND (:serviceType is null OR aau.service_type = :serviceType)
            AND ( aau.entity_code = :entityCode)
            GROUP BY ageing_duration, dashboard_currency
            ORDER BY ageing_duration
        """
    )
    fun getOutstandingByAge(serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, companyType: String?, entityCode: Int?): List<OverallAgeingStats>

    @NewSpan
    @Query(
        """
        SELECT 
        EXTRACT(MONTH FROM transaction_date) AS month,
        coalesce(sum(case when aau.acc_type in ('SINV','SDN') then sign_flag*(amount_loc - pay_loc) else 0 end),0) as open_invoice_amount,
        coalesce(sum(case when aau.acc_type in ('SINV','SDN','SCN', 'REC', 'OPDIV', 'MISC', 'BANK', 'INTER') then sign_flag*(amount_loc - pay_loc) else 0 end))  as outstandings,
        coalesce(sum(case when aau.acc_type in ('SINV','SDN','SCN') then sign_flag*amount_loc end),0) as total_sales,
        0 as days,
        0 as value,
        '' as dashboard_currency
        FROM
        ares.account_utilizations aau
        INNER JOIN organization_trade_party_details otpd on aau.organization_id = otpd.id
        INNER JOIN organizations o on o.registration_number = otpd.registration_number
        LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
        WHERE
        acc_mode  = 'AR'
        AND
        aau.entity_code = :entityCode
        AND
        EXTRACT(YEAR FROM transaction_date) = :year
        AND 
        (:serviceType is null or aau.service_type = :serviceType) 
        AND document_status in ('FINAL') 
        AND deleted_at is null
        AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds)) 
        AND (:companyType is null OR los.id is null OR los.segment = :companyType)
        group by EXTRACT(MONTH FROM transaction_date)
        """
    )
    suspend fun generateDailySalesOutstanding(year: Int, serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: String?): MutableList<DailyOutstanding>

    @NewSpan
    @Query(
        """
            SELECT to_char(date_trunc('quarter',aau.transaction_date),'Q')::int as duration,
            coalesce(sum(case when aau.acc_type in ('SINV','SDN') then sign_flag*(amount_loc - pay_loc) else 0 end),0) as open_invoice_amount,
            coalesce(sum(case when aau.acc_type in ('SINV','SDN','SCN', 'REC', 'OPDIV', 'MISC', 'BANK', 'INTER') then sign_flag*(amount_loc - pay_loc) else 0 end))  as total_outstanding_amount,
            coalesce(sum(case when aau.acc_type in ('SINV','SDN','SCN') then sign_flag*amount_loc end),0) as total_sales,
            '' as dashboard_currency
            from ares.account_utilizations aau
            INNER JOIN organization_trade_party_details otpd on aau.organization_id = otpd.id
            INNER JOIN organizations o on o.registration_number = otpd.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE 
            aau.acc_mode = 'AR' 
            AND (:serviceType is null or aau.service_type = :serviceType) 
            AND document_status in ('FINAL') 
            AND EXTRACT(YEAR FROM aau.transaction_date) = :year
            AND deleted_at is null
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            AND (aau.entity_code = :entityCode)
            AND (:companyType is null OR los.id is null OR los.segment = :companyType )
            GROUP BY date_trunc('quarter',aau.transaction_date)
        """
    )
    suspend fun generateQuarterlyOutstanding(year: Int?, serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: String?): MutableList<Outstanding>?

    @NewSpan
    @Query(
        """
            with x as (
            SELECT 
            distinct(aau.document_no) as id,
            date_trunc('day',aau.transaction_date) as duration,
            aau.amount_loc as amount,
            aau.led_currency as dashboard_currency
            from ares.account_utilizations aau
            INNER JOIN organizations o on o.id = aau.tagged_organization_id
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE 
            aau.acc_mode = 'AR' 
            AND 
            aau.document_status = 'FINAL'
            AND date_trunc('day', aau.transaction_date) >= date_trunc('day', :asOnDate:: date - '29 day'::interval)
            AND date_trunc('day', aau.transaction_date) < date_trunc('day', :asOnDate:: date + '1 day'::interval)
            AND aau.deleted_at is null 
            AND (aau.acc_type::VARCHAR = :accType)
            AND ((:defaultersOrgIds) IS NULL OR aau.organization_id NOT IN (:defaultersOrgIds))
            AND ( aau.entity_code = :entityCode)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:serviceType is null or aau.service_type = :serviceType) 
            ) SELECT x.duration, sum(x.amount) as amount, count(x.id) as count, x.dashboard_currency from x
            GROUP BY x.duration, x.dashboard_currency
            ORDER BY x.duration DESC
        """
    )
    suspend fun generateLineGraphViewDailyStats(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: String?, serviceType: ServiceType?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            SELECT 
            date_trunc('day',lj.created_at) as duration,
            coalesce(sum(CASE when invoice_type = 'INVOICE' THEN pinv.ledger_total else -1 * (pinv.ledger_total) end), 0) as amount,
            count(distinct(lj.id)) as count,
            pinv.ledger_currency as dashboard_currency
            FROM loki.jobs lj
            INNER JOIN plutus.invoices pinv on lj.id = pinv.job_id
            INNER JOIN plutus.addresses pa on pa.invoice_id = pinv.id
            INNER JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE date_trunc('day', lj.created_at) >= date_trunc('day', now():: date - '29 day'::interval)
            AND date_trunc('day', lj.created_at) <= date_trunc('day', now():: date)
            AND (:companyType is null OR los.id is null OR los.segment = :companyType )
            AND (pa.entity_code = :entityCode)
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            AND (pinv.status not in ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND (pa.organization_type = 'BUYER')
            AND o.status = 'active'
            GROUP BY date_trunc('day',lj.created_at), dashboard_currency
        """
    )
    suspend fun generateLineGraphViewShipmentCreated(asOnDate: String?, entityCode: Int?, companyType: String?, serviceType: String?): MutableList<DailySalesStats>?
}
