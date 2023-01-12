package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AresDocument
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.opentelemetry.instrumentation.annotations.WithSpan

@R2dbcRepository(dialect = Dialect.POSTGRES)

interface AresDocumentRepository : CoroutineCrudRepository<AresDocument, Long> {

    @WithSpan
    @Query(
        """
        SELECT 
          document_url
        FROM ares_documents
        WHERE id = :reportId
    """
    )
    suspend fun getSupplierOutstandingUrl(reportId: Long): String?
}
