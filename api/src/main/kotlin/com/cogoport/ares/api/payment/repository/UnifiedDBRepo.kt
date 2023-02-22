package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.InvoiceEventResponse
import com.cogoport.ares.api.common.models.SalesInvoiceResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import io.micronaut.transaction.annotation.TransactionalAdvice

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
}