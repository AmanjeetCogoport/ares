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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@TransactionalAdvice(AresConstants.UNIFIED)
@R2dbcRepository(value = AresConstants.UNIFIED, dialect = Dialect.POSTGRES)
interface UnifiedDBRepo : CoroutineCrudRepository<AccountUtilization, Long> {

    @NewSpan
    @Query(
        """
            SELECT 
            pinv.id, 
            pinv.status, 
            pinv.payment_status
            FROM 
            plutus.invoices pinv
            INNER JOIN plutus.addresses pa on pa.invoice_id = pinv.id
            INNER JOIN loki.jobs lj on lj.id = pinv.job_id
            LEFT JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on o.lead_organization_id = los.lead_organization_id
            WHERE pinv.created_at::varchar < :endDate and pinv.created_at::varchar > :startDate 
            AND pinv.status in ('DRAFT','FINANCE_ACCEPTED','IRN_GENERATED', 'POSTED') 
            AND (:cogoEntityId is null or pa.entity_code_id = :cogoEntityId)
            AND (pinv.migrated = false)
            AND (pa.organization_type = 'SELLER')
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
        """
    )
    fun getFunnelData(startDate: String, endDate: String, cogoEntityId: UUID?, companyType: String?, serviceType: String?): List<SalesInvoiceResponse>?

    @NewSpan
    @Query(
        """
            SELECT
                i.id as id,
                i.status as status,
                i.payment_status as payment_status,
                json_agg(
                    json_build_object(
                    'id',ie.id,
                    'invoice_id' , ie.invoice_id, 
                    'event_name', ie.event_name, 
                    'occurred_at',ie.occurred_at
                    )
                )::text as events
                FROM plutus.invoices i 
                INNER JOIN plutus.invoice_events ie on i.id = ie.invoice_id 
                INNER JOIN loki.jobs lj on lj.id = i.job_id
                INNER JOIN plutus.addresses pa on pa.invoice_id = i.id
                LEFT JOIN organizations o on o.registration_number = pa.registration_number
                LEFT JOIN lead_organization_segmentations los on o.lead_organization_id = los.lead_organization_id
                WHERE i.created_at::varchar < :endDate and i.created_at::varchar > :startDate
                and i.status in ('DRAFT','FINANCE_ACCEPTED','IRN_GENERATED', 'POSTED') and (migrated = false)
                AND (:cogoEntityId is null or pa.entity_code_id = :cogoEntityId)
                AND (:companyType is null or los.segment = :companyType OR los.id is null)
                AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
                AND (pa.organization_type = 'SELLER')
                GROUP BY i.id
        """
    )
    fun getInvoices(startDate: String, endDate: String, cogoEntityId: UUID?, companyType: String?, serviceType: String?): List<SalesInvoiceTimelineResponse>
    @NewSpan
    @Query(
        """
        SELECT 
        sum(-1*on_account_amount)
        FROM snapshot_organization_outstandings soo
        INNER JOIN organization_trade_party_details otpd on soo.registration_number = otpd.registration_number
        WHERE soo.registration_number is not null 
        AND soo.created_at < NOW() 
        AND ((:defaultersOrgIds) IS NULL OR otpd.id NOT IN (:defaultersOrgIds))
        AND (:entityCode is null or soo.cogo_entity = :entityCode::varchar)
        """
    )
    fun getOnAccountAmount(entityCode: Int?, defaultersOrgIds: List<UUID>? = null): BigDecimal?

    @NewSpan
    @Query(
        """
            SELECT 
                count(*) as open_invoices_count, 
                sum(CASE when tod.invoice_type = 'INVOICE' THEN open_invoice_amount else -1 * open_invoice_amount end) as open_invoice_amount,
                tod.open_invoice_currency as currency, 
                count(distinct(tod.registration_number)) as customers_count,
                shipment_service_type as service_type,
                lj.job_details  ->> 'tradeType' as trade_type,
                CASE WHEN tod.shipment_service_type in ('fcl_freight', 'lcl_freight','fcl_customs','lcl_customs','fcl_freight_local')  THEN 'ocean'
                     WHEN tod.shipment_service_type in ('air_customs', 'air_freight', 'domestic_air_freight')   THEN 'air'
                     WHEN tod.shipment_service_type in ('trailer_freight', 'haulage_freight', 'trucking', 'ltl_freight', 'ftl_freight') THEN 'surface'
                     ELSE 'others'
                END as grouped_services
            FROM temp_outstanding_invoices tod 
            INNER JOIN loki.jobs lj on lj.job_number = tod.job_number
            INNER JOIN organization_trade_party_details otpd on otpd.registration_number = tod.registration_number
            WHERE 
            tod.registration_number is not null 
            AND tod.open_invoice_amount > 0 
            AND tod.invoice_date::date <= Now()
            AND ((:defaultersOrgIds) IS NULL OR otpd.id NOT IN (:defaultersOrgIds))
            AND (:entityCode is null or tod.entity_code = :entityCode::varchar)
            GROUP BY shipment_service_type, open_invoice_currency, lj.job_details  ->> 'tradeType' 
        """
    )
    fun getOutstandingData(entityCode: Int?, defaultersOrgIds: List<UUID>? = null): List<OutstandingDocument>?

    @NewSpan
    @Query(
        """
        SELECT 
        sum(-1*on_account_amount)
        FROM snapshot_organization_outstandings soo
        INNER JOIN organization_trade_party_details otpd on soo.registration_number = otpd.registration_number
        WHERE soo.registration_number is not null 
        AND ((:defaultersOrgIds) IS NULL OR otpd.id NOT IN (:defaultersOrgIds))
        AND (:entityCode is null or soo.cogo_entity = :entityCode::varchar)
        AND date_trunc('day', soo.created_at) > date_trunc('day', NOW():: date - '7 day'::interval)
        
        """
    )
    fun getOnAccountAmountForPastSevenDays(entityCode: Int?, defaultersOrgIds: List<UUID>? = null): BigDecimal?

    @Query(
        """
            SELECT 
            sum(CASE when tod.invoice_type = 'INVOICE' THEN open_invoice_amount else -1 * open_invoice_amount end) as open_invoice_amount 
            from temp_outstanding_invoices tod 
            INNER JOIN organizations o on o.registration_number = tod.registration_number
            WHERE 
            date_trunc('day', tod.invoice_date) > date_trunc('day', NOW():: date - '7 day'::interval)
            AND (:entityCode is null or tod.entity_code = :entityCode::varchar)
            AND tod.open_invoice_amount > 0
            AND tod.registration_number is not null
        """
    )

    fun getOutstandingAmountForPastSevenDays(entityCode: Int?, defaultersOrgIds: List<UUID>? = null): BigDecimal?

    @NewSpan
    @Query(
        """
        SELECT 
        date_trunc('month',aau.transaction_date) as duration,
        coalesce(sum((aau.amount_curr)) ,0) as amount,
        aau.currency as dashboard_currency,
        COUNT(aau.id) as count
        from ares.account_utilizations aau
        INNER JOIN organization_trade_party_details otpd on aau.organization_id = otpd.id
        INNER JOIN organizations o on o.registration_number = otpd.registration_number
        LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
        WHERE aau.acc_mode = 'AR' 
        AND aau.document_status in (:docStatus)  
        AND  aau.transaction_date > :quaterStart::DATE
        AND aau.transaction_date < :quaterEnd::DATE
        AND aau.deleted_at is null 
        AND (:accType is null or  aau.acc_type = :accType)
        AND ((:defaultersOrgIds) IS NULL OR aau.organization_id NOT IN (:defaultersOrgIds))
        AND (:entityCode is null or aau.entity_code = :entityCode)
        AND (:companyType is null or los.segment = :companyType OR los.id is null)
        AND (:serviceType is null or aau.service_type::varchar = :serviceType)
        GROUP BY date_trunc('month',aau.transaction_date), dashboard_currency
        ORDER BY duration DESC
        """
    )
    suspend fun generateMonthlySalesStats(quaterStart: LocalDateTime, quaterEnd: LocalDateTime,  accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>, entityCode: Int?, companyType: String?, serviceType: ServiceType?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            SELECT date_trunc('day',aau.transaction_date) as duration,
            coalesce(sum((aau.amount_curr)) ,0) as amount,
            aau.currency as dashboard_currency,
            COUNT(aau.id) as count
            from ares.account_utilizations aau
            INNER JOIN organization_trade_party_details otpd on aau.organization_id = otpd.id
            INNER JOIN organizations o on o.registration_number = otpd.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE aau.acc_mode = 'AR' 
            AND aau.document_status in (:docStatus) 
            AND date_trunc('day', aau.transaction_date) >= date_trunc('day', :asOnDate:: date - '3 day'::interval)
            AND date_trunc('day', aau.transaction_date) < date_trunc('day', :asOnDate:: date + '1 day'::interval)
            AND aau.deleted_at is null 
            AND (:accType is null or aau.acc_type = :accType)
            AND ((:defaultersOrgIds) IS NULL OR aau.organization_id NOT IN (:defaultersOrgIds))
            AND (:entityCode is null or aau.entity_code = :entityCode)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:serviceType is null or aau.service_type::varchar = :serviceType) 
            GROUP BY date_trunc('day',aau.transaction_date), dashboard_currency
            ORDER BY duration DESC
        """
    )
    suspend fun generateDailySalesStats(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>, entityCode: Int?, companyType: String?, serviceType: ServiceType?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
        SELECT 
        date_trunc('year',aau.transaction_date) as duration,
        coalesce(sum((aau.amount_curr)) ,0) as amount,
        aau.currency as dashboard_currency,
        count(aau.id) as count
        from ares.account_utilizations aau
        INNER JOIN organization_trade_party_details otpd on aau.organization_id = otpd.id
        INNER JOIN organizations o on o.registration_number = otpd.registration_number
        LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
        WHERE 
            aau.acc_mode = 'AR' 
            AND 
            aau.document_status in (:docStatus)  
            AND date_trunc('year', aau.transaction_date) >= date_trunc('year', :asOnDate:: date - '3 year'::interval)
            AND date_trunc('year', aau.transaction_date) < date_trunc('year', :asOnDate:: date + '1 year'::interval)
            AND aau.deleted_at is null 
            AND (:accType is null or aau.acc_type = :accType)
            AND ((:defaultersOrgIds) IS NULL OR aau.organization_id NOT IN (:defaultersOrgIds))
            AND (:entityCode is null or aau.entity_code = :entityCode)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:serviceType is null or aau.service_type::varchar = :serviceType) 
        GROUP BY date_trunc('year',aau.transaction_date), dashboard_currency
        ORDER BY duration DESC
        """
    )
    suspend fun generateYearlySalesStats(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>, entityCode: Int?, companyType: String?, serviceType: ServiceType?): MutableList<DailySalesStats>?
    @NewSpan
    @Query(
        """
            SELECT 
            date_trunc('day',lj.created_at) as duration,
            coalesce(sum(CASE when invoice_type = 'INVOICE' THEN pinv.grand_total else -1 * (pinv.grand_total) end), 0) as amount,
            count(distinct(lj.id)) as count,
            pinv.currency as dashboard_currency
            from loki.jobs lj
            INNER JOIN plutus.invoices pinv on lj.id = pinv.job_id
            INNER JOIN plutus.addresses pa on pa.invoice_id = pinv.id
            INNER JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE date_trunc('day', lj.created_at) >= date_trunc('day', :asOnDate:: date - '3 day'::interval)
            AND date_trunc('day', lj.created_at) <= date_trunc('day', :asOnDate:: date)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:cogoEntityId is null or pa.entity_code_id = :cogoEntityId)
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            ANd (pinv.status not in ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND (pa.organization_type = 'SELLER')
            GROUP BY date_trunc('day',lj.created_at),dashboard_currency
        """
    )
    suspend fun generateDailyShipmentCreatedAt(asOnDate: String?, cogoEntityId: UUID?, companyType: String?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            SELECT 
            to_char(date_trunc('month',lj.created_at),'Mon') as duration,
            coalesce(sum(CASE when invoice_type = 'INVOICE' THEN pinv.grand_total else -1 * (pinv.grand_total) end), 0) as amount,
            count(distinct(lj.id)) as count,
            pinv.currency as dashboard_currency
            FROM loki.jobs lj
            INNER JOIN plutus.invoices pinv on lj.id = pinv.job_id
            INNER JOIN plutus.addresses pa on pa.invoice_id = pinv.id
            INNER JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE 
            date_trunc('month', lj.created_at) >= date_trunc('month', :asOnDate:: date - '3 month'::interval)
            and date_trunc('month', lj.created_at) <= date_trunc('month', :asOnDate:: date)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:cogoEntityId is null or pa.entity_code_id = :cogoEntityId)
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            ANd (pinv.status not in ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND (pa.organization_type = 'SELLER')
            GROUP BY date_trunc('month',lj.created_at),dashboard_currency
        """
    )
    suspend fun generateMonthlyShipmentCreatedAt(asOnDate: LocalDateTime?, cogoEntityId: UUID?, companyType: String?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            SELECT date_trunc('year',lj.created_at) as duration,
            coalesce(sum(CASE when invoice_type = 'INVOICE' THEN pinv.grand_total else -1 * (pinv.grand_total) end), 0) as amount,
            count(distinct(lj.id)) as count,
            pinv.currency as dashboard_currency
            from loki.jobs lj
            INNER JOIN plutus.invoices pinv on lj.id = pinv.job_id
            INNER JOIN plutus.addresses pa on pa.invoice_id = pinv.id
            INNER JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE date_trunc('year', lj.created_at) >= date_trunc('year', :asOnDate:: date - '3 year'::interval)
            and date_trunc('year', lj.created_at) <= date_trunc('year', :asOnDate:: date)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:cogoEntityId is null or pa.entity_code_id = :cogoEntityId)
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            ANd (pinv.status not in ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND (pa.organization_type = 'SELLER')
            GROUP BY date_trunc('year',lj.created_at), dashboard_currency
        """
    )
    suspend fun generateYearlyShipmentCreatedAt(asOnDate: String?, cogoEntityId: UUID?, companyType: String?, serviceType: String?): MutableList<DailySalesStats>?

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
            SELECT 
            coalesce(
                case 
                WHEN due_date >= now()::date then 'Not Due'
                WHEN (due_days) between 1 AND 30 then '1-30'
                WHEN (due_days) between 31 AND 60 then '31-60'
                WHEN (due_days) between 61 AND 90 then '61-90'
                WHEN (due_days) between 91 AND 180 then '91-180'
                WHEN (due_days) between 181 AND 365 then '181-365'
                WHEN (due_days) > 365 then '>365' 
                end, 'Unknown'
            ) as ageing_duration, 
            sum(toi.open_invoice_amount) as amount,
            toi.open_invoice_currency as dashboard_currency
            from temp_outstanding_invoices toi
            INNER JOIN organizations o on o.registration_number = toi.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE toi.due_date is not null 
            AND  (:companyType is null or los.segment =:companyType OR los.id is null)
            AND (:serviceType is null or toi.shipment_service_type = :serviceType)
            AND (:cogoEntityId is null or o.cogo_entity_id =:cogoEntityId)
            AND ((:defaultersOrgIds) IS NULL OR o.id NOT IN (:defaultersOrgIds))
            GROUP BY ageing_duration, dashboard_currency
            ORDER BY ageing_duration
        """
    )
    fun getOutstandingByAge(serviceType: String?, defaultersOrgIds: List<UUID>?, companyType: String?, cogoEntityId: UUID?): List<OverallAgeingStats>

    @NewSpan
    @Query(
        """
        with X as (
            SELECT 
            extract(month from date_trunc('month',(:date)::date)) as month,
            sum(case when aau.acc_type in ('SINV','SDN','SCN','SREIMB') then sign_flag*(amount_curr - pay_curr) else 0 end) as open_invoice_amount,
            abs(sum(case when aau.acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end)) as on_account_payment,
            sum(case when aau.acc_type in ('SINV','SDN','SCN','SREIMB') then sign_flag*(amount_curr - pay_curr) else 0 end) + sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end) as outstandings,
            sum(case when aau.acc_type in ('SINV','SDN','SCN','SREIMB') AND transaction_date >= date_trunc('month',(:date)::date) then sign_flag*amount_curr end) as total_sales,
            case when date_trunc('month', :date::date) < date_trunc('month', now()) then date_part('days',date_trunc('month',(:date::date + '1 month'::interval)) - '1 day'::interval) 
            else date_part('days', now()::date) end as days,
            aau.currency as dashboard_currency
            from ares.account_utilizations aau
            INNER JOIN organization_trade_party_details otpd on aau.organization_id = otpd.id
            INNER JOIN organizations o on o.registration_number = otpd.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE (:serviceType is null or aau.service_type::varchar = :serviceType) AND document_status in ('FINAL', 'PROFORMA') and acc_mode = 'AR' and transaction_date <= date_trunc('month',(:date::date + '1 month'::interval)) - '1 day'::interval and deleted_at is null
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds)) 
            AND (:entityCode is null or aau.entity_code = :entityCode)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            GROUP BY dashboard_currency
            )
            SELECT X.month, coalesce(X.open_invoice_amount,0) as open_invoice_amount, coalesce(X.on_account_payment, 0) as on_account_payment,
            coalesce(X.outstandings, 0) as outstandings, coalesce(X.total_sales,0) as total_sales, X.days,
            coalesce((case when X.total_sales != 0 then X.outstandings / X.total_sales else 0 END) * X.days,0) as value,
            X.dashboard_currency as dashboard_currency
            from X
        """
    )
    suspend fun generateDailySalesOutstanding(date: String, serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: String?): MutableList<DailyOutstanding>

    @NewSpan
    @Query(
        """
            with x as (
                SELECT extract(quarter from generate_series(CURRENT_DATE - '9 month'::interval, CURRENT_DATE, '3 month')) as quarter
            ),
            y as (
                SELECT to_char(date_trunc('quarter',aau.transaction_date),'Q')::int as quarter,
                sum(case when aau.acc_type in ('SINV','SDN','SCN','SREIMB') then sign_flag*(amount_curr - pay_curr) else 0 end) + sum(case when aau.acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end) as total_outstanding_amount,
                aau.currency as dashboard_currency
                from ares.account_utilizations aau
                INNER JOIN organization_trade_party_details otpd on aau.organization_id = otpd.id
                INNER JOIN organizations o on o.registration_number = otpd.registration_number
                LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
                WHERE aau.acc_mode = 'AR' AND (:serviceType is null or aau.service_type::varchar = :serviceType) and document_status in ('FINAL', 'PROFORMA') and date_trunc('month', transaction_date) >= date_trunc('month',CURRENT_DATE - '9 month'::interval) and deleted_at is null
                AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
                AND (:entityCode is null or aau.entity_code = :entityCode)
                AND (:companyType is null or los.segment = :companyType or los.id is null)
                GROUP BY date_trunc('quarter',transaction_date), dashboard_currency
            )
            SELECT case when x.quarter = 1 then 'Jan - Mar'
            when x.quarter = 2 then 'Apr - Jun'
            when x.quarter = 3 then 'Jul - Sep'
            when x.quarter = 4 then 'Oct - Dec' end as duration,
            coalesce(y.total_outstanding_amount, 0) as amount,
            y.dashboard_currency as dashboard_currency
            from x
            LEFT JOIN y on x.quarter = y.quarter
        """
    )
    suspend fun generateQuarterlyOutstanding(serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: String?): MutableList<Outstanding>?

    @NewSpan
    @Query(
        """
            SELECT date_trunc('day',aau.transaction_date) as duration,
            coalesce(sum((aau.amount_curr)) ,0) as amount,
            aau.currency as dashboard_currency,
            COUNT(aau.id) as count
            from ares.account_utilizations aau
            INNER JOIN organization_trade_party_details otpd on aau.organization_id = otpd.id
            INNER JOIN organizations o on o.registration_number = otpd.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE aau.acc_mode = 'AR' 
            AND aau.document_status in (:docStatus) 
            AND date_trunc('day', aau.transaction_date) >= date_trunc('day', :asOnDate:: date - '29 day'::interval) 
            AND aau.deleted_at is null 
            AND (:accType is null or aau.acc_type = :accType)
            AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
            AND (:entityCode is null or aau.entity_code = :entityCode)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:serviceType is null or aau.service_type::varchar = :serviceType) 
            GROUP BY date_trunc('day',transaction_date), dashboard_currency
        """
    )
    suspend fun generateLineGraphViewDailyStats(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>, entityCode: Int?, companyType: String?, serviceType: ServiceType?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            SELECT 
            date_trunc('day',lj.created_at) as duration,
            coalesce(sum(CASE when invoice_type = 'INVOICE' THEN pinv.grand_total else -1 * (pinv.grand_total) end), 0) as amount,
            count(distinct(lj.id)) as count,
            pinv.currency as dashboard_currency
            from loki.jobs lj
            INNER JOIN plutus.invoices pinv on lj.id = pinv.job_id
            INNER JOIN plutus.addresses pa on pa.invoice_id = pinv.id
            INNER JOIN organizations o on o.registration_number = pa.registration_number
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id
            WHERE date_trunc('day', lj.created_at) >= date_trunc('day', :asOnDate:: date - '29 day'::interval)
            AND (:companyType is null or los.segment = :companyType OR los.id is null)
            AND (:cogoEntityId is null or pa.entity_code_id = :cogoEntityId)
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            ANd (pinv.status not in ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND (pa.organization_type = 'SELLER')
            GROUP BY date_trunc('day',lj.created_at),dashboard_currency
        """
    )
    suspend fun generateLineGraphViewShipmentCreated(asOnDate: String?, cogoEntityId: UUID?, companyType: String?, serviceType: String?): MutableList<DailySalesStats>?
}
