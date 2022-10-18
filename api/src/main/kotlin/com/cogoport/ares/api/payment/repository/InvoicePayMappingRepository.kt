package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.PaymentInvoiceMapping
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface InvoicePayMappingRepository : CoroutineCrudRepository<PaymentInvoiceMapping, Long> {

    @Query(
        """
             select * from payment_invoice_mapping where document_no = :documentNo and
             (:accType is null or acc_type= :accType::account_type)
        """

    )
    suspend fun findBydocumentNo(documentNo: Long,accMode: String? = null): MutableList<PaymentInvoiceMapping>

    @Query(
        """
            update payment_invoice_mapping set deleted_at = now() where id = :id
        """
    )
    suspend fun deletedPaymentMappings(id:Long?)

}
