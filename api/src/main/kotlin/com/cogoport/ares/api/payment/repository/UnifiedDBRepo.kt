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
import com.cogoport.ares.api.payment.model.response.BillIdAndJobNumberResponse
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.PaymentDetailsAtPlatform
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
            AND sinv.status in ('DRAFT','FINANCE_ACCEPTED','IRN_GENERATED', 'POSTED', 'IRN_FAILED', 'IRN_CANCELLED') 
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
        SELECT coalesce(sum(sign_flag * (amount_loc-pay_loc)),0) as amount
        FROM ares.account_utilizations aau
        WHERE document_status = 'FINAL'
        AND (COALESCE(:entityCode) is null or aau.entity_code IN (:entityCode))
        AND aau.transaction_date < NOW() 
        AND (COALESCE(:accType) is null OR acc_type IN (:accType))
        AND (acc_mode = :accMode)
        AND (CASE WHEN :isTillYesterday = TRUE THEN aau.transaction_date < now()::DATE ELSE TRUE END)
        AND ((:defaultersOrgIds) IS NULL OR organization_id NOT IN (:defaultersOrgIds))
        AND (COALESCE(:serviceTypes) is null or aau.service_type in (:serviceTypes))
        AND (:startDate is null or :endDate is null or aau.transaction_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
        AND (CASE WHEN :accMode = 'AP' THEN aau.migrated = FALSE ELSE TRUE END)
        AND deleted_at is null
        """
    )
    fun getOnAccountAmount(
        entityCode: MutableList<Int>?,
        defaultersOrgIds: List<UUID>? = null,
        accMode: String,
        accType: List<String>?,
        serviceTypes: List<ServiceType>? = null,
        startDate: String? = null,
        endDate: String? = null,
        isTillYesterday: Boolean? = false
    ): BigDecimal?

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
            WHERE
            acc_mode = 'AR'
            AND acc_type in ('SINV', 'SCN', 'SREIMB', 'SREIMBCN')
            AND aau.document_status = 'FINAL'
            AND aau.due_date IS NOT NULL
            AND aau.transaction_date::date <= Now()
            AND ( aau.entity_code = :entityCode)
            AND (lj.job_source != 'FREIGHT_FORCE')
            AND (COALESCE(:defaultersOrgIds) IS NULL OR organization_id::UUID NOT IN (:defaultersOrgIds))
            group by aau.led_currency,aau.service_type, lj.job_details  ->> 'tradeType'
        """
    )
    fun getOpenInvoiceData(entityCode: Int?, defaultersOrgIds: List<UUID>? = null): List<OutstandingDocument>?

    @NewSpan
    @Query(
        """
            SELECT coalesce(sum((amount_loc-pay_loc)),0) as amount
            FROM ares.account_utilizations aau
            WHERE document_status = 'FINAL'
            AND (:entityCode is null OR aau.entity_code = :entityCode)
            AND aau.transaction_date < NOW() 
            AND acc_type IN ('REC', 'CTDS', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC', 'PAY')
            AND (acc_mode = 'AR')
            AND (COALESCE(:defaultersOrgIds) IS NULL OR organization_id::UUID NOT IN (:defaultersOrgIds))
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
            AND acc_type in ('SINV', 'SCN', 'SREIMB', 'SREIMBCN')
            AND (aau.entity_code = :entityCode)
            AND (COALESCE(:defaultersOrgIds) IS NULL OR organization_id::UUID NOT IN (:defaultersOrgIds))
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
            WITH y as (
            SELECT 
                DISTINCT
                aau.id,
                aau.amount_loc,
                aau.pay_loc,
                aau.sign_flag,
                aau.acc_type,
                b.kam_owners
                FROM ares.account_utilizations aau
                INNER JOIN (
                with x as
              (
              SELECT 
              s.registration_number,
              CASE WHEN self_kyc_org_id IS NOT NULL THEN self_kyc_org_id
                    WHEN self_org_id IS NOT NULL THEN self_org_id
                    WHEN self_kyc_sp_org_id IS NOT NULL THEN self_kyc_sp_org_id
                    WHEN self_sp_org_id IS NOT NULL THEN self_sp_org_id
                    ELSE paying_org_id
              END organization_id,
              CASE WHEN  (self_kyc_org_id IS NOT NULL or self_org_id IS NOT NULL or self_kyc_sp_org_id IS NOT NULL or self_sp_org_id IS NOT NULL) then 'self'
              ELSE 'paying_party'
              END trade_party_type,
              s.trade_name,
              trade_party_details_id
              FROM (
                  SELECT DISTINCT otpd.registration_number registration_number
                    ,otpd.id trade_party_details_id
                    ,otsk.id self_kyc_org_id
                    ,ots.id self_org_id
                    ,otsk_sp.id self_kyc_sp_org_id
                    ,ots_sp.id self_sp_org_id
                    ,otp.id paying_org_id, otsk.trade_name trade_name
                  FROM organization_trade_party_details otpd
                  LEFT JOIN (
                      SELECT skorg.id id
                      ,otpsk.organization_trade_party_detail_id organization_trade_party_detail_id,
                      skorg.trade_name trade_name
                      FROM organization_trade_parties otpsk
                      INNER JOIN organizations skorg ON skorg.id = otpsk.organization_id
                      WHERE skorg.account_type = 'importer_exporter'
                        AND skorg.kyc_status = 'verified'
                        AND skorg.id IS NOT NULL
                        AND otpsk.trade_party_type = 'self'
                        AND otpsk.STATUS = 'active'
                      ) otsk ON otsk.organization_trade_party_detail_id = otpd.id
                  LEFT JOIN (
                      SELECT sorg.id id
                      ,otps.organization_trade_party_detail_id organization_trade_party_detail_id
                      FROM organization_trade_parties otps
                      INNER JOIN organizations sorg ON sorg.id = otps.organization_id
                      WHERE sorg.account_type = 'importer_exporter'
                        AND sorg.id IS NOT NULL
                        AND otps.trade_party_type = 'self'
                        AND otps.STATUS = 'active'
                      ) ots ON ots.organization_trade_party_detail_id = otpd.id
                  LEFT JOIN (
                      SELECT skorg.id id
                      ,otpsk.organization_trade_party_detail_id organization_trade_party_detail_id,
                      skorg.trade_name trade_name
                      FROM organization_trade_parties otpsk
                      INNER JOIN organizations skorg ON skorg.id = otpsk.organization_id
                      WHERE skorg.account_type = 'service_provider'
                        AND skorg.kyc_status = 'verified'
                        AND skorg.id IS NOT NULL
                        AND otpsk.trade_party_type = 'self'
                        AND otpsk.STATUS = 'active'
                      ) otsk_sp ON otsk_sp.organization_trade_party_detail_id = otpd.id
                  LEFT JOIN (
                      SELECT sorg.id id
                      ,otps.organization_trade_party_detail_id organization_trade_party_detail_id
                      FROM organization_trade_parties otps
                      INNER JOIN organizations sorg ON sorg.id = otps.organization_id
                      WHERE sorg.account_type = 'service_provider'
                        AND sorg.id IS NOT NULL
                        AND otps.trade_party_type = 'self'
                        AND otps.STATUS = 'active'
                      ) ots_sp ON ots_sp.organization_trade_party_detail_id = otpd.id
                  LEFT JOIN (
                      SELECT max(DISTINCT porg.id::TEXT)::uuid id
                      ,otpp.organization_trade_party_detail_id organization_trade_party_detail_id
                      FROM organization_trade_parties otpp
                      INNER JOIN organizations porg ON porg.id = otpp.organization_id
                      WHERE porg.kyc_status = 'verified'
                        AND porg.account_type = 'importer_exporter'
                        AND porg.STATUS = 'active'
                        AND porg.id IS NOT NULL
                        AND otpp.trade_party_type = 'paying_party'
                        AND otpp.STATUS = 'active'
                        GROUP BY otpp.organization_trade_party_detail_id
                  ) otp ON otp.organization_trade_party_detail_id = otpd.id
                ) s
              )
              SELECT x.registration_number
                ,MAX(x.organization_id::text)::uuid organization_id
                ,MAX(x.trade_party_type) trade_party_type
                ,MAX(x.trade_name) trade_name
                ,MAX(o.business_name) business_name
                ,MAX(x.trade_party_details_id::text)::uuid trade_party_details_id
              from x
              INNER JOIN organizations o on o.id = x.organization_id
              LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
              WHERE (COALESCE(:companyType) is null OR los.id is null OR los.segment in (:companyType))
              GROUP BY x.registration_number) os on os.trade_party_details_id = aau.organization_id
            inner join (
                with a as
                    (
                        SELECT
                          unnest(purm.stakeholder_rm_ids) stakeholder_rm_id, stakeholder_id, os.organization_id
                        from organization_stakeholders os
                        LEFT JOIN (select distinct user_id, array_agg(reporting_manager_id) stakeholder_rm_ids from partner_user_rm_mappings where status = 'active' group by user_id) purm on os.stakeholder_id = purm.user_id
                        WHERE status='active'
                        AND os.stakeholder_type IN ('sales_agent', 'entity_manager')
                    ) 
                    SELECT
                      array_agg(distinct
                        case when stakeholder_id in (:stakeholderIds)
                        then u.name else rm_u.name end
                    ) kam_owners, a.organization_id
                from a
                INNER JOIN users u on u.id = a.stakeholder_id
                INNER JOIN users rm_u on rm_u.id = a.stakeholder_rm_id
                WHERE (
                    stakeholder_id in (:stakeholderIds)
                    or stakeholder_rm_id in (:stakeholderIds)
                )
                GROUP BY a.organization_id
                ) b on b.organization_id = os.organization_id
                WHERE aau.entity_code = :entityCode
                AND (:serviceType is null OR aau.service_type = :serviceType)
                AND ((:defaultersOrgIds) IS NULL OR aau.organization_id::UUID NOT IN (:defaultersOrgIds))
                and document_status = 'FINAL' and aau.deleted_at is null
            ) 
            SELECT
            COALESCE(ARRAY_TO_STRING(kam_owners,', '),'Others') kam_owners,
            COALESCE(sum(case when acc_type in ('SINV','SCN','SREIMB', 'SREIMBCN') then sign_flag*(amount_loc - pay_loc) else 0 end),0) as open_invoice_amount,
            COALESCE(sum(case when acc_type in ('SINV','SCN','REC', 'CTDS', 'SREIMB', 'SREIMBCN', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') then sign_flag*(amount_loc - pay_loc) else 0 end))  as total_outstanding_amount
            FROM y
            GROUP BY kam_owners
            ORDER BY total_outstanding_amount DESC
            LIMIT 10
        """
    )
    fun getKamWiseOutstanding(entityCode: Int?, serviceType: ServiceType?, companyType: List<String>?, defaultersOrgIds: List<UUID>?, stakeholderIds: List<UUID>?): List<KamWiseOutstanding>?

    @NewSpan
    @Query(
        """
            WITH z AS (
                  SELECT 
                    distinct 
                    aau.id,
                    aau.amount_loc,
                    aau.pay_loc,
                    aau.sign_flag,
                    aau.led_currency,
                    aau.due_date
                    FROM ares.account_utilizations aau
                  INNER JOIN organization_trade_party_details otpd on otpd.id = aau.organization_id
                  INNER JOIN organization_trade_parties otp ON otp.organization_trade_party_detail_id = otpd.id
                  INNER JOIN organizations o ON o.id = otp.organization_id
                  LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
                  WHERE
                    due_date is not null 
                    AND acc_mode = 'AR' 
                    AND acc_type in ('SINV','SCN','REC', 'CTDS', 'SREIMB', 'SREIMBCN', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                    AND document_status in ('FINAL') 
                    AND deleted_at is null
                    AND ((:defaultersOrgIds) IS NULL OR aau.organization_id::UUID NOT IN (:defaultersOrgIds))
                     AND (COALESCE(:companyType) is null OR COALESCE(los.segment, 'others') in (:companyType))
                    AND (:serviceType is null OR aau.service_type = :serviceType)
                    AND ( aau.entity_code = :entityCode)
                  )
            SELECT 
            coalesce(
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
            sum(sign_flag*(amount_loc-pay_loc)) as amount,
            led_currency as dashboard_currency
            FROM z
            GROUP BY ageing_duration, dashboard_currency
            ORDER BY ageing_duration
        """
    )
    fun getOutstandingByAge(serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, companyType: List<String>?, entityCode: Int?): List<OverallAgeingStats>

    @NewSpan
    @Query(
        """
            WITH b AS (SELECT 
            distinct 
            aau.id,
            transaction_date,
            amount_loc, 
            pay_loc,
            sign_flag,
            acc_type
            from ares.account_utilizations aau
            INNER JOIN organization_trade_party_details otpd on aau.organization_id::UUID = otpd.id
            INNER JOIN organizations o on o.registration_number = otpd.registration_number and o.account_type = 'importer_exporter' and o.status = 'active'
            LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
            WHERE
            acc_mode  = 'AR'
            AND
            aau.entity_code = :entityCode
            AND
            aau.document_status = 'FINAL'
            AND 
            aau.deleted_at IS NULL
            AND
            EXTRACT(YEAR FROM aau.transaction_date) = :year
            AND document_status in ('FINAL') 
            AND deleted_at is null
            AND (:serviceType is null or aau.service_type = :serviceType) 
            AND (COALESCE(:defaultersOrgIds) IS NULL OR organization_id::UUID NOT IN (:defaultersOrgIds)) 
            AND ((:companyType) is null OR los.id is null OR los.segment in (:companyType))
            )
            SELECT
            EXTRACT(MONTH FROM b.transaction_date) AS month,
            coalesce(sum(case when acc_type in ('SINV', 'SCN', 'SREIMB', 'SREIMBCN') then sign_flag*(amount_loc - pay_loc) else 0 end),0) as open_invoice_amount,
            coalesce(sum(case when acc_type in ('SINV','SCN','REC', 'CTDS', 'SREIMB', 'SREIMBCN', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') then sign_flag*(amount_loc - pay_loc) else 0 end))  as outstandings,
            coalesce(sum(case when acc_type in ('SINV','SCN', 'SREIMB', 'SREIMBCN') then sign_flag*amount_loc end),0) as total_sales,
            0 as days,
            0 as value,
            '' as dashboard_currency
            from b
            GROUP BY EXTRACT(MONTH FROM transaction_date)
        """
    )
    suspend fun generateDailySalesOutstanding(year: Int, serviceType: ServiceType?, defaultersOrgIds: List<UUID>?, entityCode: Int?, companyType: List<String>?): MutableList<DailyOutstanding>

    @NewSpan
    @Query(
        """
            WITH b AS (
                SELECT 
                    distinct 
                    aau.id,
                    transaction_date,
                    amount_loc, 
                    pay_loc,
                    sign_flag,
                    acc_type
                    from ares.account_utilizations aau
                    INNER JOIN organization_trade_party_details otpd on otpd.id = aau.organization_id::UUID 
                    INNER JOIN organizations o on o.registration_number = otpd.registration_number and o.account_type = 'importer_exporter' and o.status = 'active'
                    LEFT JOIN lead_organization_segmentations los on los.lead_organization_id = o.lead_organization_id and CASE WHEN COALESCE(:companyType) IS NULL THEN false ELSE true END
                    WHERE
                    acc_mode  = 'AR'
                    AND
                    aau.entity_code = :entityCode
                    AND
                    aau.document_status = 'FINAL'
                    AND 
                    aau.deleted_at IS NULL
                    AND
                    EXTRACT(YEAR FROM aau.transaction_date) = :year
                    AND document_status in ('FINAL') 
                    AND deleted_at is null
                    AND (:serviceType is null or aau.service_type = :serviceType) 
                    AND (COALESCE(:defaultersOrgIds) IS NULL OR organization_id::UUID NOT IN (:defaultersOrgIds)) 
                    AND ((:companyType) is null OR los.id is null OR los.segment in (:companyType))
            )
            SELECT 
            to_char(date_trunc('quarter', transaction_date),'Q')::int as duration,
            coalesce(sum(case when acc_type in ('SINV', 'SCN', 'SREIMB', 'SREIMBCN') then sign_flag*(amount_loc - pay_loc) else 0 end),0) as open_invoice_amount,
            coalesce(sum(case when acc_type in ('SINV','SCN','REC', 'CTDS', 'SREIMB', 'SREIMBCN', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') then sign_flag*(amount_loc - pay_loc) else 0 end))  as total_outstanding_amount,
            coalesce(sum(case when acc_type in ('SINV','SCN', 'SREIMB', 'SREIMBCN') then sign_flag*amount_loc end),0) as total_sales,
            '' as dashboard_currency
            FROM b 
            GROUP BY date_trunc('quarter',b.transaction_date)
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
        INNER JOIN plutus.invoices sinv on sinv.job_id = lj.id
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
			ELSE 0 END) AS three_sixty_plus_day_overdue,
       sum(
			CASE WHEN au.transaction_date < now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS till_yesterday_total_outstanding
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
		AND au.acc_type IN ('SINV','SCN','SREIMB', 'SREIMBCN')
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        AND (:startDate is null or :endDate is null or iv.invoice_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
        AND (COALESCE(:defaultersOrgIds) IS NULL OR au.organization_id::UUID NOT IN (:defaultersOrgIds))
        AND (COALESCE(:tradeType) is null or j.job_details->>'tradeType' in (:tradeType))
        """
    )
    fun getBfReceivable(
        serviceTypes: List<ServiceType>?,
        startDate: String?,
        endDate: String?,
        tradeType: List<String>?,
        entityCode: MutableList<Int>?,
        customerTypes: List<String>?,
        defaultersOrgIds: List<UUID>?
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
			ELSE 0 END) AS three_sixty_plus_day_overdue,
        sum(
			CASE WHEN au.transaction_date < now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS till_yesterday_total_outstanding
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
    sum(
		CASE WHEN iv.invoice_date::date = :date::date
			AND iv.invoice_type = 'CREDIT_NOTE' THEN
			1
		ELSE 0 END) AS total_sales_credit_notes,
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
    sum(
		CASE WHEN bill.finance_accept_date::date = :date::date
			AND bill.bill_type = 'CREDIT_NOTE' THEN
			1
		ELSE 0 END) AS total_purchase_credit_notes,
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
                     CASE WHEN :sortBy = 'profit' THEN ((SUM(j.income) - SUM(j.expense)) / SUM(j.income)) * 100 ELSE random() END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                     CASE WHEN :sortBy = 'profit' THEN ((SUM(j.income) - SUM(j.expense)) / SUM(j.income)) * 100 ELSE random() END    
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
        AND (COALESCE(:defaultersOrgIds) IS NULL OR au.organization_id::UUID NOT IN (:defaultersOrgIds))
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        """
    )
    fun getTotalRemainingAmountAR(
        accMode: AccMode,
        accType: List<AccountType>,
        serviceTypes: List<ServiceType>,
        entityCode: MutableList<Int>?,
        startDate: String?,
        endDate: String?,
        defaultersOrgIds: List<UUID>?
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
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_local_due,
        'INR' AS currency
	FROM
		ares.account_utilizations au JOIN 
        plutus.invoices iv ON au.document_no = iv.id JOIN
        loki.jobs j on j.id = iv.job_id 
	WHERE
		au.acc_mode = 'AR'
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
		AND au.deleted_at IS NULL
		AND au.acc_type IN ('SINV','SCN','SREIMB', 'SREIMBCN')
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (COALESCE(:entityCode) is null or au.entity_code IN (:entityCode))
        AND (COALESCE(:defaultersOrgIds) IS NULL OR au.organization_id::UUID NOT IN (:defaultersOrgIds))
        AND (:startDate is null or :endDate is null or iv.invoice_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
        """
    )
    fun getFinanceArCardData(
        serviceTypes: List<ServiceType>?,
        startDate: String?,
        endDate: String?,
        entityCode: MutableList<Int>?,
        defaultersOrgIds: List<UUID>?
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
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_local_due,
        'INR' AS currency
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
            WHERE status NOT IN ('DRAFT', 'FINANCE_REJECTED', 'IRN_CANCELLED')
            AND invoice_type != 'REIMBURSEMENT'
            GROUP BY job_id order by job_id desc)
        SELECT j.id FROM loki.jobs j JOIN x ON x.id = j.id WHERE
          x.total_income != j.income OR
          x.pre_tax_income != j.pre_tax_income OR  
          x.income_tax_amount != j.income_tax_amount LIMIT 1000
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
        y.pre_tax_expense != j.pre_tax_expense OR  
        y.expense_tax_amount != j.expense_tax_amount OR     
        y.total_expense != j.expense LIMIT 1000
    """
    )
    suspend fun getPurchaseAmountMismatchInJobs(): List<Long>?

    @NewSpan
    @Query(
        """
            SELECT 
            p.payment_num_value, 
            p.sage_ref_number, p.payment_document_status, soim.sage_organization_id,
            p.acc_code, p.currency, p.entity_code, p.amount, p.organization_name, 
            otpd.registration_number AS pan_number,
            p.acc_mode,
            p.trans_ref_number as utr,
            p.payment_code,
            p.transaction_date
            FROM ares.payments p
            LEFT JOIN sage_organization_id_mappings soim ON p.org_serial_id::varchar = soim.sage_organization_id::varchar
            LEFT JOIN organization_trade_party_details otpd ON otpd.serial_id::varchar = soim.trade_party_detail_serial_id::varchar and otpd.status = 'active'
            WHERE soim.status = 'active' AND
            ((p.acc_mode = 'AR' AND soim.account_type = 'importer_exporter') OR (p.acc_mode = 'AP' AND soim.account_type = 'service_provider')) AND
            p.payment_document_status IN ('POSTED', 'FINAL_POSTED')
            AND (:startDate IS NULL OR p.updated_at::VARCHAR >= :startDate)
            AND (:endDate IS NULL OR p.updated_at::VARCHAR <= :endDate)
        """
    )
    suspend fun getPaymentsByTransactionDate(startDate: String?, endDate: String?): List<PaymentDetailsAtPlatform>?

    @NewSpan
    @Query(
        """
                SELECT
                b.id, j.job_number
                FROM loki.jobs j LEFT JOIN kuber.bills b
                ON j.id = b.job_id
                WHERE b.id IN (:ids)
            """
    )
    suspend fun getJobNumbersByDocumentNos(ids: List<Long>): List<BillIdAndJobNumberResponse>
}
