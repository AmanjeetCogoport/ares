package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.PaymentInvoiceMapping
import com.cogoport.ares.api.payment.model.BillPaymentSumResponse
import com.cogoport.ares.api.payment.model.PaymentMapResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface InvoicePayMappingRepository : CoroutineCrudRepository<PaymentInvoiceMapping, Long> {

    @Query(
        """
             SELECT id,mapping_type,amount,led_amount from payment_invoice_mapping WHERE document_no = :documentNo AND account_mode = 'AP' AND payment_id = :paymentId  AND deleted_at is null
             
        """
    )
    suspend fun findByPaymentId(documentNo: Long, paymentId: Long?): PaymentMapResponse

    @Query(
        """
            UPDATE payment_invoice_mapping SET deleted_at = NOW() WHERE id = :id
        """
    )
    suspend fun deletePaymentMappings(id: Long?)

    @Query(
        """
        SELECT COALESCE(SUM(amount),0) as amount_sum ,COALESCE(SUM(led_amount),0) as ledger_amount_sum FROM payment_invoice_mapping where account_mode = 'AP' AND document_no = :documentNo AND deleted_at is null
        """
    )
    suspend fun findByDocumentNo(documentNo: Long): BillPaymentSumResponse
}
