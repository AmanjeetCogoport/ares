package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.CustomerOutstandingAgeing
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentResponse
import com.cogoport.ares.model.payment.AccountType
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
            WHERE (:query IS NULL OR document_value LIKE :query)
                AND organization_id = :organizationId
                AND acc_type = 'REC'
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
            OFFSET GREATEST(0, ((:page - 1) * :pageLimit))
            LIMIT :pageLimit  
        """
    )
    suspend fun getPaymentByTradePartyMappingId(organizationId: UUID, sortBy: String?, sortType: String?, statusList: List<DocStatus>?, query: String?, page: Int, pageLimit: Int): List<CustomerOutstandingPaymentResponse>

    @NewSpan
    @Query(
        """
            SELECT
                organization_id,
                entity_code,
                currency,
                max(organization_name) as organization_name,
                sum(
                    CASE WHEN (acc_type = :accType::account_type
                        and(due_date >= now()::date)) THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS not_due_led_amount,      
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 0 AND 30 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS thirty_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 31 AND 45 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS forty_five_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 46 AND 60 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS sixty_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS ninety_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS one_eighty_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) > 180 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS one_eighty_plus_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS total_led_outstanding,
                sum(
                    CASE WHEN (acc_type = :accType::account_type
                        and(due_date >= now()::date)) THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS not_due_curr_amount,      
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 0 AND 30 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS thirty_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 31 AND 45 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS forty_five_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 46 AND 60 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS sixty_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS ninety_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS one_eighty_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) > 180 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS one_eighty_plus_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS total_curr_outstanding,
                sum(
                    CASE WHEN due_date >= now()::date AND acc_type = :accType::account_type THEN
                        1
                    ELSE
                        0
                    END) AS not_due_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 0 AND 30 AND acc_type = :accType::account_type THEN
                        1
                    ELSE
                        0
                    END) AS thirty_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 31 AND 45 AND acc_type = :accType::account_type THEN
                        1
                    ELSE
                        0
                    END) AS forty_five_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 46 AND 60 AND acc_type = :accType::account_type THEN
                        1
                    ELSE
                        0
                    END) AS sixty_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 61 AND 90 AND acc_type = :accType::account_type THEN
                        1
                    ELSE
                        0
                    END) AS ninety_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 AND acc_type = :accType::account_type THEN
                        1
                    ELSE
                        0
                    END) AS one_eighty_count,
                sum(
                    CASE WHEN (now()::date - due_date) > 180 AND acc_type = :accType::account_type THEN
                        1
                    ELSE
                        0
                    END) AS one_eighty_plus_count
            FROM
                account_utilizations
            WHERE
                acc_mode = 'AR'
                AND due_date IS NOT NULL
                AND document_status = 'FINAL'
                AND organization_id IS NOT NULL
                AND amount_curr - pay_curr > 0
                AND entity_code = :entityCode
                AND (:orgId IS NULL OR organization_id = :orgId::uuid)
                AND acc_type = :accType::account_type
                AND deleted_at IS NULL
            GROUP BY
                organization_id, entity_code, currency 
        """
    )
    suspend fun getInvoicesOutstandingAgeingBucket(entityCode: Int, accType: AccountType, orgId: String?): List<CustomerOutstandingAgeing>
}
