package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.PaymentInvoiceMapping
import com.cogoport.ares.api.payment.model.PaymentMapResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface InvoicePayMappingRepository : CoroutineCrudRepository<PaymentInvoiceMapping, Long> {

    @NewSpan
    @Query(
        """
             SELECT id,mapping_type,amount,led_amount from payment_invoice_mapping WHERE document_no = :documentNo AND account_mode = 'AP' AND payment_id = :paymentId  AND
             (CASE 
                  WHEN :deletedAt = false  THEN deleted_at is null 
             END)
             
        """
    )
    suspend fun findByPaymentId(documentNo: Long, paymentId: Long?, deletedAt: Boolean): PaymentMapResponse

    @NewSpan
    @Query(
        """
            UPDATE payment_invoice_mapping SET deleted_at = NOW() WHERE id = :id
        """
    )
    suspend fun deletePaymentMappings(id: Long?)

    @NewSpan
    @Query(
        """
            select coalesce(count(*), 0) from payment_invoice_mapping where payment_id=:paymentId
        """
    )
    suspend fun findByPaymentIdFromPaymentInvoiceMapping(paymentId: Long?): Long

    @NewSpan
    @Query(
        """
             SELECT id,mapping_type,amount,led_amount from payment_invoice_mapping WHERE document_no = :documentNo AND account_mode = 'AP' AND payment_id = :paymentId
             
        """
    )
    suspend fun findByPaymentIdForTaggedBills(documentNo: Long, paymentId: Long?): PaymentMapResponse
}
