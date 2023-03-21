package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentResponse
import com.cogoport.ares.model.payment.DocStatus
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepo : CoroutineCrudRepository<AccountUtilization, Long> {
    @NewSpan
    @Query(
        """ 
        SELECT
        *
        FROM (
            SELECT
                acc_code,
                acc_mode,
                amount_curr AS payment_amount,
                amount_loc,
                pay_curr AS utilized_amount,
                pay_loc AS payment_loc,
                created_at,
                currency,
                entity_code,
                led_currency AS ledger_currency,
                organization_name,
                document_no,
                document_value AS payment_number,
                sign_flag,
                transaction_date,
                updated_at,
                (
                    CASE WHEN pay_curr = 0 THEN
                        'UNUTILIZED'
                    WHEN amount_curr = pay_curr THEN
                        'PARTIAL_UTILIZED'
                    ELSE
                        'UTILIZED'
                    END
                ) utilization_status
            FROM
                account_utilizations
            WHERE (document_value LIKE :query
                AND trade_party_mapping_id = :tradePartyMappingId)
        ) subquery
        WHERE
            utilization_status::varchar IN (:statusList)
        ORDER BY
            CASE WHEN :sortBy = 'transactionDate'
                THEN CASE WHEN :sortType = 'Asc' THEN subquery.transaction_date END
            END ASC,
            CASE WHEN :sortBy = 'transactionDate'
                THEN CASE WHEN :sortType = 'Desc' THEN subquery.transaction_date END
            END DESC,
            CASE WHEN :sortBy = 'paymentAmount'
                THEN CASE WHEN :sortType = 'Asc' THEN subquery.payment_amount END
            END ASC,
            CASE WHEN :sortBy = 'paymentAmount'
                THEN CASE WHEN :sortType = 'Desc' THEN subquery.payment_amount END
            END DESC   
        
        """
    )
    suspend fun getPaymentByTradePartyMappingId(tradePartyMappingId: UUID, sortBy: String?, sortType: String?, statusList: List<DocStatus>?, query: String?): List<CustomerOutstandingPaymentResponse>
}
