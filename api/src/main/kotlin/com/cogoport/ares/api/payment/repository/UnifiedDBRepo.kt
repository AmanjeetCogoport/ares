package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.InvoiceEventResponse
import com.cogoport.ares.api.common.models.OutstandingDocument
import com.cogoport.ares.api.common.models.SalesInvoiceResponse
import com.cogoport.ares.api.common.models.SalesInvoiceTimelineResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.Audit
import com.cogoport.ares.api.payment.entity.Outstanding
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
                sum(open_invoice_amount) as open_invoice_amount,
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
    suspend fun generateMonthlyOutstanding(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>): MutableList<Outstanding>?

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
    suspend fun generateDailySalesOutstanding(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>): MutableList<Outstanding>?

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
    suspend fun generateYearlySalesOutstanding(asOnDate: String, accType: String, defaultersOrgIds: List<UUID>?, docStatus: List<String>): MutableList<Outstanding>?

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
    suspend fun generateDailyShipmentCreatedAt (asOnDate: String?): MutableList<Outstanding>?

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
    suspend fun generateMonthlyShipmentCreatedAt (asOnDate: String?): MutableList<Outstanding>?

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
    suspend fun generateYearlyShipmentCreatedAt (asOnDate: String?): MutableList<Outstanding>?


}
