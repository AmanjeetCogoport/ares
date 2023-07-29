package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.ARLedgerJobDetailsResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.model.payment.AccMode
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import io.micronaut.transaction.annotation.TransactionalAdvice
import java.sql.Timestamp

@TransactionalAdvice(AresConstants.UNIFIED)
@R2dbcRepository(value = AresConstants.UNIFIED, dialect = Dialect.POSTGRES)
interface UnifiedDBNewRepository : CoroutineCrudRepository<AccountUtilization, Long> {
    @NewSpan
    @Query(
        """
            SELECT au.transaction_date::varchar AS transaction_date,
            au.acc_type as document_type,
            au.document_value::varchar AS document_number,
            au.currency as currency,
            au.amount_curr::varchar as amount,
            CASE WHEN au.sign_flag < 0 THEN au.amount_loc ELSE 0 END AS credit,
            CASE WHEN au.sign_flag > 0 THEN au.amount_loc ELSE 0 END AS debit,
            p.trans_ref_number AS transaction_ref_number,
            j.job_details -> 'documentDetails' as job_documents,
            '' as shipment_document_number
            FROM ares.account_utilizations au
            LEFT JOIN ares.payments p ON p.payment_num = au.document_no
            LEFT JOIN plutus.invoices i ON i.invoice_number = au.document_value::varchar and i.id = au.document_no
            LEFT JOIN loki.jobs j ON j.id = i.job_id
            WHERE au.acc_mode = :accMode AND au.organization_id = :organizationId::UUID AND document_status = 'FINAL'
            AND au.transaction_date >= :startDate::DATE AND au.transaction_date <= :endDate::DATE AND au.entity_code IN (:entityCodes)
            AND au.deleted_at IS NULL AND au.acc_type != 'NEWPR'
            ORDER BY transaction_date
        """
    )
    suspend fun getARLedger(accMode: AccMode, organizationId: String, entityCodes: List<Int>, startDate: Timestamp, endDate: Timestamp): List<ARLedgerJobDetailsResponse>
}
