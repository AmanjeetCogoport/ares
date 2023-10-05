package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AresDocument
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AresDocumentRepository : CoroutineCrudRepository<AresDocument, Long> {

    @NewSpan
    @Query(
        """
        SELECT 
          document_url
        FROM ares_documents
        WHERE id = :reportId
    """
    )
    suspend fun getSupplierOutstandingUrl(reportId: Long): String?

    @NewSpan
    @Query(
        """
            SELECT EXISTS(SELECT * FROM ares_documents WHERE document_url = :documentUrl) 
        """
    )
    suspend fun existsByDocumentUrl(documentUrl: String): Boolean
}
