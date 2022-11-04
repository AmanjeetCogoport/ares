package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.PaymentInvoiceMapping
import com.cogoport.ares.api.payment.model.PaymentMapResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface InvoicePayMappingRepository : CoroutineCrudRepository<PaymentInvoiceMapping, Long> {

    @Query(
        """
             select id,payment_id from payment_invoice_mapping where document_no = :documentNo and account_mode = 'AP' and deleted_at is null
             
        """
    )
    suspend fun findBydocumentNo(documentNo: Long): List<PaymentMapResponse>

    @Query(
        """
            update payment_invoice_mapping set deleted_at = now() where id = :id
        """
    )
    suspend fun deletePaymentMappings(id: Long?)
}
