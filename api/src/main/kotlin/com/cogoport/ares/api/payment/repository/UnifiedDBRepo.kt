package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.OutstandingDocument
import com.cogoport.ares.api.common.models.SalesInvoiceResponse
import com.cogoport.ares.api.common.models.SalesInvoiceTimelineResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.BfCustomerProfitabilityResp
import com.cogoport.ares.api.payment.entity.BfReceivableAndPayable
import com.cogoport.ares.api.payment.entity.BfShipmentProfitabilityResp
import com.cogoport.ares.api.payment.entity.DailyOutstanding
import com.cogoport.ares.api.payment.entity.DailySalesStats
import com.cogoport.ares.api.payment.entity.KamWiseOutstanding
import com.cogoport.ares.api.payment.entity.LogisticsMonthlyData
import com.cogoport.ares.api.payment.entity.Outstanding
import com.cogoport.ares.api.payment.entity.OverallAgeingStats
import com.cogoport.ares.api.payment.entity.ProfitCountResp
import com.cogoport.ares.api.payment.entity.ServiceWiseCardData
import com.cogoport.ares.api.payment.entity.TodayPurchaseStats
import com.cogoport.ares.api.payment.entity.TodaySalesStat
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
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
            distinct (sinv.id) as id, 
            sinv.status, 
            sinv.payment_status
            FROM 
            plutus.invoices sinv
            INNER JOIN loki.jobs lj on lj.id = sinv.job_id
            INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
            LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
            LEFT JOIN organizations o on o.id = pb.organization_id
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
            WHERE 
            EXTRACT(YEAR FROM COALESCE(sinv.invoice_date, sinv.proforma_date)) = :year
            AND EXTRACT(MONTH FROM COALESCE(sinv.invoice_date, sinv.proforma_date)) = :month
            AND sinv.status in ('DRAFT','FINANCE_ACCEPTED','IRN_GENERATED', 'POSTED') 
            AND (pa.entity_code = :entityCode)
            AND lj.job_source != 'FREIGHT_FORCE'
            AND sinv.invoice_type in ('INVOICE', 'CREDIT_NOTE')
            AND (sinv.migrated = false)
            AND ((:companyType) is null OR los.id is null OR los.segment in (:companyType))
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
        """
    )
    fun getFunnelData(entityCode: Int?, companyType: List<String>?, serviceType: String?, year: Int?, month: Int?): List<SalesInvoiceResponse>?

    @NewSpan
    @Query(
        """
            SELECT 
            distinct(sinv.id) as id, 
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
            plutus.invoices sinv
            INNER JOIN loki.jobs lj on lj.id = sinv.job_id
            INNER JOIN plutus.invoice_events ie on ie.invoice_id = sinv.id
            INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
            LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
            LEFT JOIN organizations o on o.id = pb.organization_id
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
            WHERE 
            EXTRACT(YEAR FROM COALESCE(sinv.invoice_date, sinv.proforma_date)) = :year
            AND EXTRACT(MONTH FROM COALESCE(sinv.invoice_date, sinv.proforma_date)) = :month
            AND sinv.status in ('DRAFT','FINANCE_ACCEPTED','IRN_GENERATED', 'POSTED') 
            AND (pa.entity_code = :entityCode)
            AND sinv.invoice_type in ('INVOICE', 'CREDIT_NOTE')
            AND (sinv.migrated = false)
            AND lj.job_source != 'FREIGHT_FORCE'
            AND ((:companyType) is null OR los.id is null OR los.segment in (:companyType))
            AND (:serviceType is null or lj.job_details ->> 'shipmentType' = :serviceType)
            group by sinv.id, sinv.status, sinv.payment_status
        """
    )
    fun getInvoiceTatStats(year: Int?, month: Int?, entityCode: Int?, companyType: List<String>?, serviceType: String?): List<SalesInvoiceTimelineResponse>
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
            AND aau.document_status = 'FINAL'
            AND aau.migrated = false
            AND (amount_loc-pay_loc) > 0 
            AND aau.transaction_date::date <= Now()
            AND (:entityCode is null or aau.entity_code = :entityCode)
            AND lj.job_source != 'FREIGHT_FORCE'
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
            AND document_status = 'FINAL'
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
           WITH months AS (
                SELECT generate_series(:quarterStart::date, :quarterEnd::date - '1 month'::interval, '1 month'::interval)::date AS duration
                ), x AS (
          select 
                sinv.invoice_type as invoice_type,
                sinv.id as id,
                sinv.ledger_total as amount,
                sinv.ledger_currency as dashboard_currency,
                date_trunc('day', sinv.invoice_date) AS duration
                from plutus.invoices sinv
                INNER JOIN loki.jobs lj on sinv.job_id = lj.id
                INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
                LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
                LEFT JOIN organizations o on o.id = pb.organization_id
                LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
                WHERE 
                pa.entity_code = :entityCode
                AND sinv.status in ('POSTED','FINANCE_ACCEPTED','IRN_GENERATED','FAILED','IRN_FAILED')
                AND (COALESCE(:companyType) is null OR los.segment in (:companyType))
                AND lj.job_source != 'FREIGHT_FORCE'
                and sinv.invoice_date is not null
                AND sinv.invoice_date >= :quarterStart ::DATE
                AND sinv.invoice_date < :quarterEnd::DATE
                AND (COALESCE(:invoiceType) is NULL OR sinv.invoice_type in (:invoiceType))
                AND (:serviceType IS NULL OR lj.job_details ->> 'shipmentType' = :serviceType)
          )
        SELECT 
          months.duration,
          COALESCE(SUM(x.amount), 0) AS amount, 
          COALESCE(COUNT(x.id), 0) AS count, 
          x.dashboard_currency,
          x.invoice_type
        FROM 
          months 
          LEFT JOIN x ON extract(month from months.duration) = extract (month from x.duration) and extract(year from months.duration) = extract(year from x.duration)
        GROUP BY 
          months.duration, 
          x.dashboard_currency,
          x.invoice_type
        ORDER BY 
          months.duration ASC
        """
    )
    suspend fun generateMonthlySalesStats(quarterStart: LocalDateTime, quarterEnd: LocalDateTime, invoiceType: List<String>?, entityCode: Int?, companyType: List<String>?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
    WITH date_series AS (
    SELECT generate_series(
       date_trunc('day', :asOnDate::date - '3 day'::interval),
       date_trunc('day', :asOnDate::date),
       '1 day'::interval
    ) AS date), x AS (
        select 
        sinv.invoice_type as invoice_type,
        sinv.id as id,
        sinv.ledger_total as amount,
        sinv.ledger_currency as dashboard_currency,
        sinv.invoice_date  as duration
        from plutus.invoices sinv
        INNER JOIN loki.jobs lj on sinv.job_id = lj.id
        INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
        LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
        LEFT JOIN organizations o on o.id = pb.organization_id
        LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
        WHERE 
        pa.entity_code = :entityCode
        AND sinv.status in ('POSTED','FINANCE_ACCEPTED','IRN_GENERATED','FAILED','IRN_FAILED')
        AND (COALESCE(:companyType) is null OR los.segment in (:companyType))
        AND lj.job_source != 'FREIGHT_FORCE'
        and sinv.invoice_date is not null
        AND((:invoiceType) is null or sinv.invoice_type IN (:invoiceType))
        AND (:serviceType IS NULL OR lj.job_details ->> 'shipmentType' = :serviceType)
          )
        SELECT
          date_series.date AS duration,
          COALESCE(SUM(x.amount), 0) AS amount,
          COALESCE(COUNT(x.id), 0) AS count,
          x.dashboard_currency,
          x.invoice_type
        FROM date_series
        LEFT JOIN x ON x.duration = date_series.date
        GROUP BY date_series.date, x.dashboard_currency, x.invoice_type
        ORDER BY date_series.date ASC
        """
    )
    suspend fun generateDailySalesStats(asOnDate: String, invoiceType: List<String>?, entityCode: Int?, companyType: List<String>?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
        WITH date_series AS (
    SELECT generate_series(
       date_trunc('year', :asOnDate::date - '3 year'::interval),
       date_trunc('year', :asOnDate::date),
       interval '1 year'
    ) AS duration), x AS (
          select 
            sinv.invoice_type as invoice_type,
            sinv.id as id,
            sinv.ledger_total as amount,
            sinv.ledger_currency as dashboard_currency,
            sinv.invoice_date  as duration
            from plutus.invoices sinv
            INNER JOIN loki.jobs lj on sinv.job_id = lj.id
            INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
            LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
            LEFT JOIN organizations o on o.id = pb.organization_id
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
            WHERE 
            pa.entity_code = :entityCode
            AND sinv.status in ('POSTED','FINANCE_ACCEPTED','IRN_GENERATED','FAILED','IRN_FAILED')
            AND (COALESCE(:companyType) is null OR los.segment in (:companyType))
            AND lj.job_source != 'FREIGHT_FORCE'
            and sinv.invoice_date is not null
            AND((:invoiceType) is null or sinv.invoice_type IN (:invoiceType))
            AND (:serviceType IS NULL OR lj.job_details ->> 'shipmentType' = :serviceType)
        ), series_with_data AS (
          SELECT 
            ds.duration, 
            COALESCE(SUM(x.amount), 0) AS amount, 
            COUNT(x.id) AS count, 
            x.dashboard_currency,
            invoice_type
          FROM date_series ds
          LEFT JOIN x ON extract (year from x.duration) = extract (year from ds.duration)
          GROUP BY ds.duration, x.dashboard_currency, invoice_type
          ORDER BY ds.duration ASC
        )
        SELECT * FROM series_with_data
        """
    )
    suspend fun generateYearlySalesStats(asOnDate: String, invoiceType: List<String>?, entityCode: Int?, companyType: List<String>?, serviceType: String?): MutableList<DailySalesStats>?
    @NewSpan
    @Query(
        """
            WITH date_series AS (
                SELECT generate_series(
                   date_trunc('day', :asOnDate::date - '3 day'::interval),
                   date_trunc('day', :asOnDate::date),
                   interval '1 day'
                ) AS duration
            ), x AS (
              SELECT 
                date_trunc('day', lj.created_at) AS duration,
                coalesce(sum(CASE WHEN invoice_type = 'INVOICE' THEN sinv.ledger_total ELSE -1 * (sinv.ledger_total) END), 0) AS amount,
                count(DISTINCT lj.id) AS count,
                sinv.ledger_currency AS dashboard_currency,
                '' as invoice_type
              FROM loki.jobs lj
              INNER JOIN  plutus.invoices sinv on sinv.job_id = lj.id
              INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
              LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
              LEFT JOIN organizations o on o.id = pb.organization_id
              LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
              WHERE date_trunc('day', lj.created_at) >= date_trunc('day', :asOnDate:: date - '3 day'::interval)
                AND date_trunc('day', lj.created_at) <= date_trunc('day', :asOnDate:: date)
                AND ((:companyType) IS NULL OR los.id IS NULL OR los.segment IN (:companyType))
                AND (pa.entity_code = :entityCode)
                AND lj.job_source != 'FREIGHT_FORCE'
                AND (:serviceType IS NULL OR lj.job_details ->> 'shipmentType' = :serviceType)
                AND (sinv.status NOT IN ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
              GROUP BY date_trunc('day', lj.created_at), dashboard_currency
            ), series_with_data AS (
              SELECT 
                ds.duration, 
                COALESCE(x.amount, 0) AS amount, 
                COALESCE(x.count, 0) AS count, 
                x.dashboard_currency,
                x.invoice_type
              FROM date_series ds
              LEFT JOIN x ON x.duration = ds.duration
            )
            SELECT * FROM series_with_data
            ORDER BY duration ASC
        """
    )
    suspend fun generateDailyShipmentCreatedAt(asOnDate: String?, entityCode: Int?, companyType: List<String>?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            WITH date_series AS (
                SELECT generate_series(
                   date_trunc('month', :quarterStart::date),
                   date_trunc('month', :quarterEnd::date - '1 month'::interval),
                   interval '1 month'
                ) AS duration
            ), x AS (
          SELECT 
            date_trunc('month',lj.created_at) AS duration,
            coalesce(sum(CASE WHEN invoice_type = 'INVOICE' THEN sinv.ledger_total ELSE -1 * (sinv.ledger_total) END), 0) AS amount,
            count(distinct lj.id) AS count,
            sinv.ledger_currency AS dashboard_currency,
            '' as invoice_type
          FROM loki.jobs lj
          INNER JOIN plutus.invoices sinv on sinv.job_id = lj.id
          INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
          LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
          LEFT JOIN organizations o on o.id = pb.organization_id
          LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
          WHERE 
            lj.created_at > :quarterStart::DATE
            AND lj.created_at < :quarterEnd::DATE
            AND ((:companyType) IS NULL OR los.id IS NULL OR los.segment IN (:companyType))
            AND (pa.entity_code = :entityCode)
            AND (:serviceType IS NULL OR lj.job_details ->> 'shipmentType' = :serviceType)
            AND (sinv.status NOT IN ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND lj.job_source != 'FREIGHT_FORCE'
          GROUP BY date_trunc('month',lj.created_at), dashboard_currency
        ), series_with_data AS (
          SELECT 
            ds.duration, 
            COALESCE(x.amount, 0) AS amount, 
            COALESCE(x.count, 0) AS count, 
            x.dashboard_currency,
            x.invoice_type
          FROM date_series ds
          LEFT JOIN x ON x.duration = ds.duration
        )
        SELECT * FROM series_with_data
        ORDER BY duration
        """
    )
    suspend fun generateMonthlyShipmentCreatedAt(quarterStart: LocalDateTime, quarterEnd: LocalDateTime, entityCode: Int?, companyType: List<String>?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            WITH date_series AS (
                SELECT generate_series(
                   date_trunc('year', :asOnDate::date - '3 year'::interval),
                   date_trunc('year', :asOnDate::date),
                   interval '1 year'
                ) AS duration
            ), x AS (
          SELECT 
            date_trunc('year', lj.created_at) AS duration,
            coalesce(sum(CASE WHEN invoice_type = 'INVOICE' THEN sinv.ledger_total ELSE -1 * (sinv.ledger_total) END), 0) AS amount,
            count(DISTINCT lj.id) AS count,
            sinv.ledger_currency AS dashboard_currency,
            '' as invoice_type
          FROM loki.jobs lj
          INNER JOIN plutus.invoices sinv on sinv.job_id = lj.id
          INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
          LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
          LEFT JOIN organizations o on o.id = pb.organization_id
          LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
          WHERE date_trunc('year', lj.created_at) >= date_trunc('year', :asOnDate:: date - '3 year'::interval)
            AND date_trunc('year', lj.created_at) <= date_trunc('year', :asOnDate:: date)
            AND ((:companyType) IS NULL OR los.id IS NULL OR los.segment IN (:companyType))
            AND (pa.entity_code = :entityCode)
            AND lj.job_source != 'FREIGHT_FORCE'
            AND (:serviceType IS NULL OR lj.job_details ->> 'shipmentType' = :serviceType)
            AND (sinv.status NOT IN ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
          GROUP BY date_trunc('year', lj.created_at), dashboard_currency
        ), series_with_data AS (
          SELECT 
            ds.duration, 
            COALESCE(x.amount, 0) AS amount, 
            COALESCE(x.count, 0) AS count, 
            x.dashboard_currency,
            x.invoice_type
          FROM date_series ds
          LEFT JOIN x ON x.duration = ds.duration
        )
        SELECT * FROM series_with_data
        ORDER BY duration ASC
        """
    )
    suspend fun generateYearlyShipmentCreatedAt(asOnDate: String?, entityCode: Int?, companyType: List<String>?, serviceType: String?): MutableList<DailySalesStats>?

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
            AND ((:companyType) is null OR los.id is null OR los.segment in (:companyType))
            AND (:serviceType is null OR aau.service_type = :serviceType)
            AND ( aau.entity_code = :entityCode)
            GROUP BY ageing_duration, dashboard_currency
            ORDER BY ageing_duration
        """
    )
    fun getOutstandingByAge(serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, companyType: List<String>?, entityCode: Int?): List<OverallAgeingStats>

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
        EXTRACT(YEAR FROM aau.transaction_date) = :year
        AND 
        (:serviceType is null or aau.service_type = :serviceType) 
        AND document_status in ('FINAL') 
        AND deleted_at is null
        AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds)) 
        AND ((:companyType) is null OR los.id is null OR los.segment in (:companyType))
        group by EXTRACT(MONTH FROM transaction_date)
        """
    )
    suspend fun generateDailySalesOutstanding(year: Int, serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: List<String>?): MutableList<DailyOutstanding>

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
            AND ((:companyType) is null OR los.id is null OR los.segment in (:companyType))
            GROUP BY date_trunc('quarter',aau.transaction_date)
        """
    )
    suspend fun generateQuarterlyOutstanding(year: Int?, serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: List<String>?): MutableList<Outstanding>?

    @NewSpan
    @Query(
        """
        WITH date_series AS (
    SELECT generate_series(
       date_trunc('day', :asOnDate::date - '29 day'::interval),
       date_trunc('day', :asOnDate::date),
       '1 day'::interval
    ) AS date), x AS (
        select 
        sinv.invoice_type as invoice_type,
        sinv.id as id,
        sinv.ledger_total as amount,
        sinv.ledger_currency as dashboard_currency,
        sinv.invoice_date  as duration
        from plutus.invoices sinv
        INNER JOIN loki.jobs lj on sinv.job_id = lj.id
        INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
        LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
        LEFT JOIN organizations o on o.id = pb.organization_id
        LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
        WHERE 
        pa.entity_code = :entityCode
        AND sinv.status in ('POSTED','FINANCE_ACCEPTED','IRN_GENERATED','FAILED','IRN_FAILED')
        AND (COALESCE(:companyType) is null OR los.segment in (:companyType))
        AND lj.job_source != 'FREIGHT_FORCE'
        and sinv.invoice_date is not null
        AND((:invoiceType) is null or sinv.invoice_type IN (:invoiceType))
        AND (:serviceType IS NULL OR lj.job_details ->> 'shipmentType' = :serviceType)
          )
        SELECT
          date_series.date AS duration,
          COALESCE(SUM(x.amount), 0) AS amount,
          COALESCE(COUNT(x.id), 0) AS count,
          x.dashboard_currency,
          x.invoice_type
        FROM date_series
        LEFT JOIN x ON x.duration = date_series.date
        GROUP BY date_series.date, x.dashboard_currency, x.invoice_type
        ORDER BY date_series.date ASC
        """
    )
    suspend fun generateLineGraphViewDailyStats(asOnDate: String, invoiceType: List<String>?, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: List<String>?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            WITH date_series AS (
              SELECT generate_series(
                       date_trunc('day', :asOnDate::date - '29 day'::interval),
                       date_trunc('day', :asOnDate::date),
                       interval '1 day'
                     ) AS duration
            ), x AS (
          SELECT 
            date_trunc('day', lj.created_at) AS duration,
            coalesce(sum(CASE WHEN invoice_type = 'INVOICE' THEN sinv.ledger_total ELSE -1 * (sinv.ledger_total) END), 0) AS amount,
            count(DISTINCT lj.id) AS count,
            sinv.ledger_currency AS dashboard_currency,
            '' as invoice_type
          FROM loki.jobs lj
        inner join plutus.invoices sinv on sinv.job_id = lj.id
          INNER JOIN plutus.addresses pa on pa.invoice_id = sinv.id and pa.organization_type = 'SELLER'
          LEFT JOIN plutus.addresses pb on pb.invoice_id = sinv.id and pb.organization_type = 'BOOKING_PARTY'
          LEFT JOIN organizations o on o.id = pb.organization_id
          LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
          WHERE date_trunc('day', lj.created_at) >= date_trunc('day', :asOnDate:: date - '29 day'::interval)
            AND date_trunc('day', lj.created_at) <= date_trunc('day', :asOnDate:: date)
            AND ((:companyType) IS NULL OR los.id IS NULL OR los.segment IN (:companyType))
            AND (pa.entity_code = :entityCode)
            AND (:serviceType IS NULL OR lj.job_details ->> 'shipmentType' = :serviceType)
            AND (sinv.status NOT IN ('FINANCE_REJECTED', 'CONSOLIDATED', 'IRN_CANCELLED'))
            AND lj.job_source != 'FREIGHT_FORCE'
          GROUP BY date_trunc('day', lj.created_at), dashboard_currency
        ), series_with_data AS (
          SELECT 
            ds.duration, 
            COALESCE(x.amount, 0) AS amount, 
            COALESCE(x.count, 0) AS count, 
            x.dashboard_currency,
            x.invoice_type
          FROM date_series ds
          LEFT JOIN x ON x.duration = ds.duration
        )
        SELECT * FROM series_with_data
        ORDER BY duration ASC
        """
    )
    suspend fun generateLineGraphViewShipmentCreated(asOnDate: String?, entityCode: Int?, companyType: List<String>?, serviceType: String?): MutableList<DailySalesStats>?

    @NewSpan
    @Query(
        """
            SELECT
		sum(
			CASE WHEN au.due_date >= now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS non_overdue_amount,
		sum(
			CASE WHEN au.due_date < now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS overdue_amount,
		sum(
			CASE WHEN ((au.amount_loc - au.pay_loc) > 0
				AND au.acc_type IN ('SINV','SREIMB')) THEN
				1
			ELSE 0 END) AS not_paid_document_count,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 1 AND 30 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS thirty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 31 AND 60 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS sixty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 61 AND 90 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS ninety_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 91 AND 180 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS one_eighty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 181 AND 360 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS three_sixty_day_overdue,
        sum(
			CASE WHEN (now()::date - au.due_date) > 360 THEN
				sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS three_sixty_plus_day_overdue
	FROM
		ares.account_utilizations au JOIN 
        plutus.invoices iv ON au.document_no = iv.id JOIN
        loki.jobs j on j.id = iv.job_id 
        LEFT JOIN organizations o ON au.tagged_organization_id = o.id
        LEFT JOIN lead_organization_segmentations los ON los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:customerTypes) IS NULL THEN false ELSE true END
	WHERE
		au.acc_mode = 'AR'
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
        AND (COALESCE(:customerTypes) is null OR los.segment in(:customerTypes))
		AND au.deleted_at IS NULL
		AND au.acc_type IN ('SINV','SCN','SREIMB')
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        AND (:startDate is null or :endDate is null or iv.invoice_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
        AND (COALESCE(:tradeType) is null or j.job_details->>'tradeType' in (:tradeType))
        """
    )
    fun getBfReceivable(
        serviceTypes: List<ServiceType>?,
        startDate: String?,
        endDate: String?,
        tradeType: List<String>?,
        entityCode: MutableList<Int>?,
        customerTypes: List<String>?
    ): BfReceivableAndPayable

    @NewSpan
    @Query(
        """
            SELECT
		sum(
			CASE WHEN au.due_date >= now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS non_overdue_amount,
		sum(
			CASE WHEN au.due_date < now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS overdue_amount,
		sum(
			CASE WHEN ((au.amount_loc - au.pay_loc) > 0
				AND au.acc_type IN ('PINV','PREIMB')) THEN
				1
			ELSE 0 END) AS not_paid_document_count,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 1 AND 30 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS thirty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 31 AND 60 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS sixty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 61 AND 90 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS ninety_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 91 AND 180 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS one_eighty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 181 AND 360 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS three_sixty_day_overdue,
        sum(
			CASE WHEN (now()::date - au.due_date) > 360 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS three_sixty_plus_day_overdue
	FROM
		ares.account_utilizations au JOIN 
        kuber.bills bill ON au.document_no = bill.id JOIN
        loki.jobs j on j.id = bill.job_id
	WHERE
		au.acc_mode = 'AP'
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
		AND au.deleted_at IS NULL
		AND au.acc_type IN ('PINV','PCN','PREIMB')
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        AND (:startDate is null or :endDate is null or bill.finance_accept_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
        AND (COALESCE(:tradeType) is null or j.job_details->>'tradeType' in (:tradeType))
        """
    )
    fun getBfPayable(
        serviceTypes: List<ServiceType>?,
        startDate: String?,
        endDate: String?,
        tradeType: List<String>?,
        entityCode: MutableList<Int>?,
    ): BfReceivableAndPayable

    @NewSpan
    @Query(
        """
            SELECT
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:endYear, '-01-01')::DATE
			AND CONCAT(:endYear, '-01-31')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS january,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:endYear, '-02-01')::DATE
			AND CONCAT(:endYear, CASE WHEN :isLeapYear = TRUE THEN '-02-29' ELSE '-02-28' END)::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS february,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:endYear, '-03-01')::DATE
			AND CONCAT(:endYear, '-03-31')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS march,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-04-01')::DATE
			AND CONCAT(:startYear, '-04-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS april,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-05-01')::DATE
			AND CONCAT(:startYear, '-05-31')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS may,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-06-01')::DATE
			AND CONCAT(:startYear, '-06-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS june,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-07-01')::DATE
			AND CONCAT(:startYear, '-07-31')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
               CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS july,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-08-01')::DATE
			AND CONCAT(:startYear, '-08-31')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS august,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-09-01')::DATE
			AND CONCAT(:startYear, '-09-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS september,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-10-01')::DATE
			AND CONCAT(:startYear, '-10-31')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS october,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-11-01')::DATE
			AND CONCAT(:startYear, '-11-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS november,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-12-01')::DATE
			AND CONCAT(:startYear, '-12-31')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE (inv.ledger_total/inv.grand_total) * inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * (inv.ledger_total/inv.grand_total) * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS december
FROM
	plutus.invoices inv
	JOIN ares.account_utilizations au ON au.document_no = inv.id
		AND au.acc_mode = 'AR'
        AND au.acc_type IN ('SINV','SCN')
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes))
        AND au.document_status = 'FINAL'
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        AND inv.status NOT IN ('DRAFT','FINANCE_REJECTED','IRN_CANCELLED','CONSOLIDATED')
        AND au.tagged_organization_id NOT IN ('ee09645b-5f34-4d2e-8ec7-6ac83a7946e1')
        """
    )
    fun getBfIncomeMonthly(serviceTypes: List<ServiceType>?, startYear: String, endYear: String, isPostTax: Boolean, entityCode: MutableList<Int>?, isLeapYear: Boolean): LogisticsMonthlyData
    @NewSpan
    @Query(
        """
            SELECT
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:endYear, '-01-01')::DATE
			AND CONCAT(:endYear, '-01-31')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                 CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS january,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:endYear, '-02-01')::DATE
			AND CONCAT(:endYear, CASE WHEN :isLeapYear = TRUE THEN '-02-29' ELSE '-02-28' END)::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS february,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:endYear, '-03-01')::DATE
			AND CONCAT(:endYear, '-03-31')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS march,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:startYear, '-04-01')::DATE
			AND CONCAT(:startYear, '-04-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS april,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:startYear, '-05-01')::DATE
			AND CONCAT(:startYear, '-05-31')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS may,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:startYear, '-06-01')::DATE
			AND CONCAT(:startYear, '-06-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS june,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:startYear, '-07-01')::DATE
			AND CONCAT(:startYear, '-07-31')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS july,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:startYear, '-08-01')::DATE
			AND CONCAT(:startYear, '-08-31')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS august,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:startYear, '-09-01')::DATE
			AND CONCAT(:startYear, '-09-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS september,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:startYear, '-10-01')::DATE
			AND CONCAT(:startYear, '-10-31')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS october,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:startYear, '-11-01')::DATE
			AND CONCAT(:startYear, '-11-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS november,
	sum(
		CASE WHEN finance_accept_date::DATE BETWEEN CONCAT(:startYear, '-12-01')::DATE
			AND CONCAT(:startYear, '-12-31')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE (bill.ledger_total/bill.grand_total) * bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * (bill.ledger_total/bill.grand_total) * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS december
FROM
	kuber.bills bill
	LEFT JOIN ares.account_utilizations au ON au.document_no = bill.id
		where au.acc_mode = 'AP'
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes))
        AND au.document_status = 'FINAL'
        AND au.acc_type IN ('PINV','PCN')
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        AND bill.status NOT IN ('INITIATED','COE_REJECTED','FINANCE_REJECTED','DRAFT','LOCKED')
        """
    )
    fun getBfExpenseMonthly(serviceTypes: List<ServiceType>?, startYear: String, endYear: String, isPostTax: Boolean, entityCode: MutableList<Int>?, isLeapYear: Boolean): LogisticsMonthlyData
    @NewSpan
    @Query(
        """
    SELECT
	sum(
		CASE WHEN iv.invoice_date::date = :date::date THEN
			CASE WHEN iv.invoice_type = 'INVOICE' THEN
				iv.ledger_total
			WHEN iv.invoice_type = 'CREDIT_NOTE' THEN
				- 1 * iv.ledger_total
			ELSE 0 END
		ELSE 0 END) AS total_revenue,
	sum(
		CASE WHEN iv.invoice_date::date = :date::date
			AND iv.invoice_type = 'INVOICE' THEN
			1
		ELSE 0 END) AS total_invoices,
	count(DISTINCT CASE WHEN au.acc_type = 'SINV' THEN
			au.tagged_organization_id
		ELSE NULL END) AS total_sales_orgs
    FROM
	plutus.invoices iv
	JOIN ares.account_utilizations au ON iv.id = au.document_no
    WHERE
    au.acc_mode = 'AR'
    AND au.acc_type IN ('SINV','SCN')
    AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
    AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
	AND iv.invoice_date::date = :date::date
    AND iv.status NOT IN ('DRAFT','FINANCE_REJECTED','IRN_CANCELLED','CONSOLIDATED')       
     """
    )
    fun getSalesStatsByDate(serviceTypes: List<ServiceType>?, entityCode: MutableList<Int>?, date: LocalDate): TodaySalesStat

    @NewSpan
    @Query(
        """
    SELECT
	sum(
		CASE WHEN bill.finance_accept_date::date = :date::date THEN
			CASE WHEN bill.bill_type = 'BILL' THEN
				bill.ledger_total
			WHEN bill.bill_type = 'CREDIT_NOTE' THEN
				- 1 * bill.ledger_total
			ELSE 0 END
		ELSE 0 END) AS total_expense,
	sum(
		CASE WHEN bill.finance_accept_date::date = :date::date
			AND bill.bill_type = 'BILL' THEN
			1
		ELSE 0 END) AS total_bills,
	count(DISTINCT CASE WHEN au.acc_type = 'PINV' THEN
			au.tagged_organization_id
		ELSE NULL END) AS total_purchase_orgs
    FROM
	kuber.bills bill
	JOIN ares.account_utilizations au ON bill.id = au.document_no
    WHERE
    au.acc_mode = 'AP'
    AND au.acc_type IN ('PINV','PCN')
	AND bill.finance_accept_date::date = :date::date	
    AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
    AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
	AND bill.status NOT IN ('INITIATED','COE_REJECTED','FINANCE_REJECTED','DRAFT','LOCKED')
     """
    )
    fun getPurchaseStatsByDate(serviceTypes: List<ServiceType>?, entityCode: MutableList<Int>?, date: LocalDate): TodayPurchaseStats
    @NewSpan
    @Query(
        """
            SELECT
	j.job_number,
	j.job_details->>'shipmentType' AS shipment_type,
	o.business_name,
	j.tagged_entity_id AS tagged_entity_id,
	s.state AS shipment_milestone,
	j.income AS income,
	j.expense AS expense,
    j.profit_percent AS profitability,
    LOWER(j.state) AS job_status
FROM
	loki.jobs j
	JOIN shipments s ON j.job_number::VARCHAR = s.serial_id::VARCHAR
	JOIN organizations o ON o.id = s.importer_exporter_id
WHERE
	j.income != 0
	AND j.expense != 0
    AND (:query IS NULL OR (o.business_name ILIKE :query OR j.job_number ILIKE :query))
    AND (COALESCE(:serviceType) is null or UPPER(j.job_details->>'shipmentType') in (:serviceType))
    AND (COALESCE(:taggedEntityId) IS NULL OR j.tagged_entity_id::VARCHAR IN (:taggedEntityId))
    AND (:startDate is null or :endDate is null or s.created_at::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
    AND (:jobStatus IS NULL OR j.state = :jobStatus)
    ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                     CASE WHEN :sortBy = 'createdAt' THEN EXTRACT(epoch FROM j.created_at)::numeric
                         WHEN :sortBy = 'profit' THEN j.profit_percent
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                     CASE WHEN :sortBy = 'createdAt' THEN EXTRACT(epoch FROM j.created_at)::numeric
                         WHEN :sortBy = 'profit' THEN j.profit_percent
                    END        
            END 
            Asc
    OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
    """
    )
    fun listShipmentProfitability(
        page: Int,
        pageLimit: Int,
        query: String?,
        jobStatus: String?,
        sortBy: String?,
        sortType: String?,
        taggedEntityId: MutableList<String>?,
        startDate: String?,
        endDate: String?,
        serviceType: List<ServiceType>?
    ): List<BfShipmentProfitabilityResp>

    @NewSpan
    @Query(
        """
    SELECT COUNT(*) AS total_count ,sum(j.profit_percent)/COUNT(*) AS average_profit
    FROM
	loki.jobs j
	JOIN shipments s ON j.job_number::VARCHAR = s.serial_id::VARCHAR
	JOIN organizations o ON o.id = s.importer_exporter_id
    WHERE
	j.income != 0
	AND j.expense != 0
    AND (COALESCE(:serviceType) is null or UPPER(j.job_details->>'shipmentType') in (:serviceType))
    AND (:startDate is null or :endDate is null or s.created_at::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
    AND (COALESCE(:taggedEntityId) IS NULL OR j.tagged_entity_id::VARCHAR IN (:taggedEntityId))
    AND (:query IS NULL OR (o.business_name ILIKE :query OR j.job_number ILIKE :query))
    AND (:jobStatus IS NULL OR j.state = :jobStatus)     
        """
    )
    fun findTotalCountShipment(query: String?, jobStatus: String?, taggedEntityId: MutableList<String>?, startDate: String?, endDate: String?, serviceType: List<ServiceType>?): ProfitCountResp
    @NewSpan
    @Query(
        """
    SELECT
	count(DISTINCT s.serial_id) AS shipment_count,
	s.importer_exporter_id,o.sage_company_id as entity,
	o.business_name,sum(j.income) AS booked_income,sum(j.expense) AS booked_expense,
    ((SUM(j.income) - SUM(j.expense)) / SUM(j.income)) * 100 as profitability

FROM
	loki.jobs j
	JOIN shipments s ON j.job_number::varchar = s.serial_id::VARCHAR
	JOIN organizations o ON o.id = s.importer_exporter_id
WHERE
	o.account_type = 'importer_exporter'
	AND j.income != 0
	AND j.expense != 0
    AND (COALESCE(:entityCode) is null or o.sage_company_id::DECIMAL IN (:entityCode))
    AND (:query IS NULL OR o.business_name ILIKE :query)
GROUP BY
	s.importer_exporter_id,
	o.business_name,
	o.sage_company_id
        ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                     CASE WHEN :sortBy = 'profit' THEN ((SUM(j.income) - SUM(j.expense)) / 100) ELSE random() END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                     CASE WHEN :sortBy = 'profit' THEN ((SUM(j.income) - SUM(j.expense)) / 100) ELSE random() END    
            END 
            Asc
    OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
    """
    )
    fun listCustomerProfitability(page: Int, pageLimit: Int, query: String?, sortBy: String?, sortType: String?, entityCode: MutableList<Int>?): List<BfCustomerProfitabilityResp>
    @NewSpan
    @Query(
        """
             SELECT
             COUNT(DISTINCT s.importer_exporter_id) AS total_count,
             (((SUM(j.income) - SUM(j.expense)) / SUM(j.income)) * 100) /  COUNT(DISTINCT s.importer_exporter_id) AS average_profit
FROM
	loki.jobs j
	JOIN shipments s ON j.job_number::VARCHAR = s.serial_id::VARCHAR
	JOIN organizations o ON o.id = s.importer_exporter_id
WHERE
	o.account_type = 'importer_exporter'
	AND j.income != 0
	AND j.expense != 0
    AND (COALESCE(:entityCode) is null or o.sage_company_id::DECIMAL IN (:entityCode))
	AND s.state != 'cancelled'
    AND (:query IS NULL OR o.business_name ILIKE :query)  
        """
    )
    fun findTotalCountCustomer(query: String?, entityCode: MutableList<Int>?): ProfitCountResp

    @Query(
        """
             SELECT
		sum(au.sign_flag * (au.amount_loc - au.pay_loc)) 
	FROM
		ares.account_utilizations au 
        JOIN plutus.invoices inv ON au.document_no = inv.id
	WHERE
		au.acc_mode = :accMode
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
        AND (:startDate is null or :endDate is null or inv.invoice_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
		AND au.deleted_at IS NULL
		AND au.acc_type IN (:accType)
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        """
    )
    fun getTotalRemainingAmountAR(
        accMode: AccMode,
        accType: List<AccountType>,
        serviceTypes: List<ServiceType>,
        entityCode: MutableList<Int>?,
        startDate: String?,
        endDate: String?
    ): BigDecimal?
    @Query(
        """
             SELECT
		sum(au.sign_flag * (au.amount_loc - au.pay_loc)) 
	FROM
		ares.account_utilizations au 
        JOIN kuber.bills bill ON au.document_no = bill.id

	WHERE
		au.acc_mode = :accMode
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
        AND (:startDate is null or :endDate is null or bill.finance_accept_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
		AND au.deleted_at IS NULL
		AND au.acc_type IN (:accType)
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes))
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        """
    )
    fun getTotalRemainingAmountAP(
        accMode: AccMode,
        accType: List<AccountType>,
        serviceTypes: List<ServiceType>,
        entityCode: MutableList<Int>?,
        startDate: String?,
        endDate: String?
    ): BigDecimal?

    @Query(
        """
        SELECT
		sum(
			CASE WHEN au.due_date < now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS total_overdue,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('import','IMPORT')
               AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_import_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('export','EXPORT')
            AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_export_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('domestic','LOCAL')
           AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_other_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('domestic')
            AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_domestic_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('LOCAL')
            AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_local_due
	FROM
		ares.account_utilizations au JOIN 
        plutus.invoices iv ON au.document_no = iv.id JOIN
        loki.jobs j on j.id = iv.job_id 
	WHERE
		au.acc_mode = 'AR'
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
		AND au.deleted_at IS NULL
		AND au.acc_type IN ('SINV','SCN','SREIMB')
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        AND (:startDate is null or :endDate is null or iv.invoice_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
        """
    )
    fun getFinanceArCardData(
        serviceTypes: List<ServiceType>?,
        startDate: String?,
        endDate: String?,
        entityCode: MutableList<Int>?,
    ): ServiceWiseCardData

    @Query(
        """
        SELECT
		sum(
			CASE WHEN au.due_date < now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS total_overdue,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('import','IMPORT')
              AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_import_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('export','EXPORT')
             AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_export_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('domestic','LOCAL')
            AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_other_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('domestic')
            AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_domestic_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('LOCAL')
            AND au.due_date < now()::date THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_local_due
	FROM
		ares.account_utilizations au JOIN 
        kuber.bills bill ON au.document_no = bill.id JOIN
        loki.jobs j on j.id = bill.job_id 
	WHERE
		au.acc_mode = 'AP'
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
		AND au.deleted_at IS NULL
		AND au.acc_type IN ('PINV','PCN','PREIMB')
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        AND (:startDate is null or :endDate is null or bill.finance_accept_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
        """
    )
    fun getFinanceApCardDate(
        serviceTypes: List<ServiceType>?,
        startDate: String?,
        endDate: String?,
        entityCode: MutableList<Int>?,
    ): ServiceWiseCardData

    @NewSpan
    @Query(
        """
            SELECT inv.id FROM plutus.invoices inv 
            JOIN loki.jobs j on j.id = inv.job_id
            LEFT JOIN ares.account_utilizations ac 
            ON inv.id = ac.document_no AND ac.acc_mode = 'AR'
            WHERE ac.id IS NULL
            AND inv.status NOT IN ('FINANCE_REJECTED', 'IRN_CANCELLED', 'CONSOLIDATED')
            AND j.job_source != 'FREIGHT_FORCE';
        """
    )
    suspend fun getInvoicesNotPresentInAres(): List<Long>?

    @NewSpan
    @Query(
        """
            SELECT inv.id FROM ares.account_utilizations au 
            INNER JOIN plutus.invoices inv ON inv.id = au.document_no AND acc_type IN ('SINV', 'SCN')
            WHERE inv.grand_total != au.amount_curr OR inv.ledger_total != au.amount_loc;
        """
    )
    suspend fun getInvoicesAmountMismatch(): List<Long>?

    @NewSpan
    @Query(
        """
            SELECT ac.id FROM ares.account_utilizations ac 
            LEFT JOIN plutus.invoices inv ON inv.id = ac.document_no 
            WHERE inv.id IS NULL AND document_status = 'PROFORMA' 
            AND ac.acc_mode = 'AR' AND ac.acc_type IN ('SINV', 'SCN');
        """
    )
    suspend fun getInvoicesNotPresentInPlutus(): List<Long>?

    @NewSpan
    @Query(
        """
        WITH x AS
         (SELECT 
            job_id as id,
            COALESCE(SUM(CASE WHEN invoice_type NOT IN ('CREDIT_NOTE') THEN sub_total * exchange_rate ELSE -1 * sub_total * exchange_rate END),0) as pre_tax_income,
            COALESCE(SUM(CASE WHEN invoice_type NOT IN ('CREDIT_NOTE') THEN tax_total * exchange_rate ELSE -1 * tax_total * exchange_rate END),0) as income_tax_amount,
            COALESCE(SUM(CASE WHEN invoice_type NOT IN ('CREDIT_NOTE') THEN grand_total * exchange_rate ELSE -1 * grand_total * exchange_rate END),0) as total_income 
            FROM plutus.invoices 
            WHERE status NOT IN ('DRAFT', 'FINANCE_REJECTED', 'IRN_CANCELLED', 'CONSOLIDATED')
            AND invoice_type != 'REIMBURSEMENT'
            GROUP BY job_id order by job_id desc)
        SELECT j.id FROM loki.jobs j JOIN x ON x.id = j.id WHERE
          ABS(x.total_income - j.income) >= 0 OR
          ABS(x.pre_tax_income - j.pre_tax_income) >= 0 OR  
          ABS(x.income_tax_amount - j.income_tax_amount) >= 0 LIMIT 1000
         """
    )
    suspend fun getSalesAmountMismatchInJobs(): List<Long>?

    @NewSpan
    @Query(
        """
        WITH y AS
         (SELECT 
            job_id AS id,
            COALESCE(SUM(CASE WHEN bill_type != 'CREDIT_NOTE' THEN sub_total * exchange_rate ELSE -1 * sub_total * exchange_rate END),0) as pre_tax_expense,
            COALESCE(SUM(CASE WHEN bill_type != 'CREDIT_NOTE' THEN tax_total * exchange_rate ELSE -1 * tax_total * exchange_rate END),0) as expense_tax_amount,
            COALESCE(SUM(CASE WHEN bill_type != 'CREDIT_NOTE' THEN grand_total * exchange_rate ELSE -1 * grand_total * exchange_rate END),0) as total_expense
            FROM kuber.bills 
            WHERE status NOT IN ('INITIATED', 'DRAFT', 'ON_HOLD', 'LOCKED', 'FINANCE_REJECTED', 'COE_REJECTED', 'VOID')
            AND bill_type NOT IN ('REIMBURSEMENT', 'EXPENSE', 'REIMBURSEMENT_CN')
            group by job_id order by job_id desc)
        SELECT j.id FROM loki.jobs j JOIN y ON y.id = j.id WHERE
        ABS(y.pre_tax_expense - j.pre_tax_expense) >= 0 OR  
        ABS(y.expense_tax_amount - j.expense_tax_amount) >= 0 OR     
        ABS(y.total_expense - j.expense) >= 0 LIMIT 1000
    """
    )
    suspend fun getPurchaseAmountMismatchInJobs(): List<Long>?
}
