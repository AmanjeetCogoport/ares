package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.InvoiceEventResponse
import com.cogoport.ares.api.common.models.OutstandingDocument
import com.cogoport.ares.api.common.models.SalesInvoiceResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import io.micronaut.transaction.annotation.TransactionalAdvice
import java.util.Date

@TransactionalAdvice(AresConstants.UNIFIED)
@R2dbcRepository(value = AresConstants.UNIFIED, dialect = Dialect.POSTGRES)
interface UnifiedDBRepo : CoroutineCrudRepository<AccountUtilization, Long> {

    @NewSpan
    @Query(
        """
            select id, status, payment_status from plutus.invoices where created_at::varchar < :endDate and created_at::varchar > :startDate
        """
    )
    fun getFunnelData (startDate:String, endDate: String): List<SalesInvoiceResponse>?

    @NewSpan
    @Query(
        """
            select id,invoice_id, event_name, created_at, updated_at, occurred_at  from plutus.invoice_events where invoice_id = :invoiceId
        """
    )
    fun getInvoiceEvents (invoiceId: Long): List<InvoiceEventResponse>?
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
            where registration_number is not null and open_invoice_amount > 0 and shipment_service_type is not null and invoice_date < :asOnDate  and  lj.job_details  ->> 'tradeType' != '' and tod.shipment_service_type !=''
            group by shipment_service_type, open_invoice_currency, lj.job_details  ->> 'tradeType' 
        """
    )
    fun getOutstandingData (asOnDate: Date): List<OutstandingDocument>?
    
}