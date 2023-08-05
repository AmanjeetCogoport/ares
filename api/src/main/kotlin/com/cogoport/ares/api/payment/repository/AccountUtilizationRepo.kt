package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.dunning.model.response.DunningInvoices
import com.cogoport.ares.api.dunning.model.response.DunningPayments
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.CustomerOutstandingAgeing
import com.cogoport.ares.api.payment.entity.LedgerSummary
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentResponse
import com.cogoport.ares.api.payment.model.response.DocumentResponse
import com.cogoport.ares.api.settlement.entity.Document
import com.cogoport.ares.model.common.TradePartyOutstandingRes
import com.cogoport.ares.model.dunning.response.CustomerOutstandingAndOnAccountResponse
import com.cogoport.ares.model.dunning.response.DunningCardData
import com.cogoport.ares.model.dunning.response.MonthWiseStatisticsOfAccountUtilizationResponse
import com.cogoport.ares.model.dunning.response.OverallOutstandingAndOnAccountResponse
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import com.cogoport.ares.model.payment.response.CustomerMonthlyPayment
import com.cogoport.ares.model.payment.response.LedgerDetails
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Date
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepo : CoroutineCrudRepository<AccountUtilization, Long> {
    @NewSpan
    @Query(
        """
            UPDATE account_utilizations SET 
            tagged_bill_id = CASE WHEN tagged_bill_id IS NULL THEN :taggedBillIds
                                        ELSE CONCAT(tagged_bill_id, ', ',:taggedBillIds)
                                   END
            WHERE document_no = :documentNo
            and acc_mode = 'AP' and is_void = false and deleted_at is null
        """
    )
    suspend fun updateTaggedBillIds(documentNo: Long, taggedBillIds: String)

    @NewSpan
    @Query(
        """ 
            select
                *
            from
                account_utilizations 
            where 
                document_no in (:documentNo) and acc_type::varchar in (:accType)
            and document_status != 'DELETED'::document_status
            and (:accMode is null or acc_mode = :accMode::account_mode)
            and deleted_at is null 
            order by updated_at desc
        """
    )
    suspend fun findRecords(documentNo: List<Long>, accType: List<String?>, accMode: String? = null): MutableList<AccountUtilization>

    @NewSpan
    @Query(
        """
            UPDATE account_utilizations SET 
              updated_at = now(), is_void = :isVoid WHERE document_no in (:ids) and acc_mode = 'AP' AND deleted_at is null"""
    )
    suspend fun updateAccountUtilizations(ids: List<Long>, isVoid: Boolean)
    @NewSpan
    @Query(

        """
            UPDATE account_utilizations SET 
            tds_amount = :tdsAmount, tds_amount_loc = :tdsAmountLoc 
            WHERE document_no = :documentNo and acc_mode = 'AP' AND deleted_at is null
        """
    )
    suspend fun updateTdsAmount(documentNo: Long, tdsAmount: BigDecimal, tdsAmountLoc: BigDecimal)

    @NewSpan
    @Query(
        """ 
        SELECT
        *
        FROM (
            SELECT
                au.acc_code,
                p.sage_ref_number,
                au.acc_mode,
                au.amount_curr AS payment_amount,
                au.amount_loc,
                au.pay_curr AS utilized_amount,
                au.pay_loc AS payment_loc,
                au.created_at,
                au.currency,
                au.entity_code,
                au.led_currency AS ledger_currency,
                au.organization_name,
                au.document_no,
                au.document_value AS payment_number,
                au.sign_flag,
                au.transaction_date,
                au.updated_at,
                (
                    CASE WHEN au.pay_curr = 0 THEN
                        'UNUTILIZED'
                    WHEN (au.amount_curr - au.pay_curr) > 0 THEN
                        'PARTIAL_UTILIZED'
                    ELSE
                        'UTILIZED'
                    END
                ) utilization_status
            FROM
                account_utilizations au
		        JOIN payments p ON au.document_value = p.payment_num_value
            WHERE (:query IS NULL OR au.document_value LIKE :query 
                    OR p.sage_ref_number LIKE :query)
                AND au.organization_id = :organizationId
                AND au.acc_type in ('REC', 'CTDS')
                AND au.entity_code = :entityCode
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
    suspend fun getPaymentByTradePartyMappingId(organizationId: UUID, sortBy: String?, sortType: String?, statusList: List<DocStatus>?, query: String?, entityCode: Int, page: Int, pageLimit: Int): List<CustomerOutstandingPaymentResponse>
    @NewSpan
    @Query(
        """
            UPDATE account_utilizations SET pay_curr = (pay_curr - :payCurr), pay_loc = (pay_loc - :payLoc) WHERE id = :id
        """
    )
    suspend fun markPaymentUnutilized(id: Long, payCurr: BigDecimal, payLoc: BigDecimal)

    @NewSpan
    @Query(
        """UPDATE account_utilizations SET 
              pay_curr = :currencyPay , pay_loc = :ledgerPay , updated_at = NOW() WHERE document_no =:documentNo AND acc_type = :accType::account_type AND deleted_at is null"""
    )
    suspend fun updateAccountUtilizationByDocumentNo(documentNo: Long, currencyPay: BigDecimal, ledgerPay: BigDecimal, accType: AccountType?)

    @NewSpan
    @Query(
        """
            SELECT
                organization_id,
                entity_code,
                currency,
                max(organization_name) as organization_name,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(due_date > now()::date) THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS not_due_led_amount,      
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 0 AND 30 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS thirty_led_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 31 AND 45 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS forty_five_led_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 46 AND 60 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS sixty_led_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS ninety_led_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS one_eighty_led_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) > 180 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS one_eighty_plus_led_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 181 AND 365 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS three_sixty_five_led_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) > 365 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS three_sixty_five_plus_led_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType) THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS total_led_outstanding,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(due_date > now()::date) THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS not_due_curr_amount,      
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 0 AND 30 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS thirty_curr_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 31 AND 45 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS forty_five_curr_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 46 AND 60 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS sixty_curr_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS ninety_curr_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS one_eighty_curr_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) > 180 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS one_eighty_plus_curr_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) BETWEEN 181 AND 365 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS three_sixty_five_curr_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType)
                        and(now()::date - due_date) > 365 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS three_sixty_five_plus_curr_amount,
                sum(
                    CASE WHEN acc_type::varchar IN (:accType) THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS total_curr_outstanding,
                sum(
                    CASE WHEN due_date > now()::date AND acc_type::varchar IN (:accType) AND amount_curr - pay_curr <> 0 THEN
                        1
                    ELSE
                        0
                    END) AS not_due_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 0 AND 30 AND acc_type::varchar IN (:accType) AND amount_curr - pay_curr <> 0 THEN
                        1
                    ELSE
                        0
                    END) AS thirty_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 31 AND 45 AND acc_type::varchar IN (:accType) AND amount_curr - pay_curr <> 0 THEN
                        1
                    ELSE
                        0
                    END) AS forty_five_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 46 AND 60 AND acc_type::varchar IN (:accType) AND amount_curr - pay_curr <> 0 THEN
                        1
                    ELSE
                        0
                    END) AS sixty_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 61 AND 90 AND acc_type::varchar IN (:accType) AND amount_curr - pay_curr <> 0 THEN
                        1
                    ELSE
                        0
                    END) AS ninety_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 AND acc_type::varchar IN (:accType) AND amount_curr - pay_curr <> 0 THEN
                        1
                    ELSE
                        0
                    END) AS one_eighty_count,
                sum(
                    CASE WHEN (now()::date - due_date) > 180 AND acc_type::varchar IN (:accType) AND amount_curr - pay_curr <> 0 THEN
                        1
                    ELSE
                        0
                    END) AS one_eighty_plus_count,
                sum(
                    CASE WHEN (now()::date - due_date) BETWEEN 180 AND 365 AND acc_type::varchar IN (:accType) AND amount_curr - pay_curr <> 0 THEN
                        1
                    ELSE
                        0
                    END) AS three_sixty_five_count,
                sum(
                    CASE WHEN (now()::date - due_date) > 365 AND acc_type::varchar IN (:accType) AND amount_curr - pay_curr <> 0 THEN
                        1
                    ELSE
                        0
                    END) AS three_sixty_five_plus_count
            FROM
                account_utilizations
            WHERE
                acc_mode = 'AR'
                AND due_date IS NOT NULL
                AND document_status = 'FINAL'
                AND organization_id IS NOT NULL
                AND entity_code = :entityCode
                AND (:orgId IS NULL OR organization_id = :orgId::uuid)
                AND acc_type::varchar IN (:accType)
                AND deleted_at IS NULL
            GROUP BY
                organization_id, entity_code, currency 
        """
    )
    suspend fun getOutstandingAgeingBucket(entityCode: Int, accType: List<String>, orgId: String?): List<CustomerOutstandingAgeing>

    @NewSpan
    @Query(
        """
            SELECT
                organization_id,
                entity_code,
                currency,
                max(organization_name) as organization_name,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(transaction_date > now()::date) THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS not_due_led_amount,      
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 0 AND 30 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS thirty_led_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 31 AND 45 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS forty_five_led_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 46 AND 60 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS sixty_led_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 61 AND 90 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS ninety_led_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 91 AND 180 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS one_eighty_led_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) > 180 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS one_eighty_plus_led_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 181 AND 365 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS three_sixty_five_led_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) > 365 THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS three_sixty_five_plus_led_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') THEN
                        sign_flag * (amount_loc - pay_loc)
                    ELSE
                        0
                    END) AS total_led_outstanding,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(transaction_date > now()::date) THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS not_due_curr_amount,      
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 0 AND 30 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS thirty_curr_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 31 AND 45 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS forty_five_curr_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 46 AND 60 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS sixty_curr_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 61 AND 90 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS ninety_curr_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 91 AND 180 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS one_eighty_curr_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) > 180 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS one_eighty_plus_curr_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) BETWEEN 181 AND 365 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS three_sixty_five_curr_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                        and(now()::date - transaction_date) > 365 THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS three_sixty_five_plus_curr_amount,
                sum(
                    CASE WHEN acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') THEN
                        sign_flag * (amount_curr - pay_curr)
                    ELSE
                        0
                    END) AS total_curr_outstanding,
                SUM(
                    CASE 
                        WHEN transaction_date > now()::date 
                            AND acc_type IN ('REC', 'CTDS') 
                            AND ABS(amount_curr - pay_curr) > 0.001 THEN 1 
                        WHEN transaction_date > now()::date 
                            AND acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                            AND amount_curr - pay_curr <> 0 THEN 1 
                        ELSE 0 
                    END
                )  AS not_due_count,
                SUM(
                    CASE 
                        WHEN (now()::date - transaction_date) BETWEEN 0 AND 30 
                            AND acc_type IN ('REC', 'CTDS') 
                            AND ABS(amount_curr - pay_curr) > 0.001 THEN 1 
                        WHEN (now()::date - transaction_date) BETWEEN 0 AND 30 
                            AND acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                            AND amount_curr - pay_curr <> 0 THEN 1 
                        ELSE 0 
                    END
                ) AS thirty_count,
                SUM(
                    CASE 
                        WHEN (now()::date - transaction_date) BETWEEN 31 AND 45 
                            AND acc_type IN ('REC', 'CTDS') 
                            AND ABS(amount_curr - pay_curr) > 0.001 THEN 1 
                        WHEN (now()::date - transaction_date) BETWEEN 31 AND 45 
                            AND acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                            AND amount_curr - pay_curr <> 0 THEN 1 
                        ELSE 0 
                    END
                ) AS forty_five_count,
                SUM(
                    CASE 
                        WHEN (now()::date - transaction_date) BETWEEN 46 AND 60 
                            AND acc_type IN ('REC', 'CTDS') 
                            AND ABS(amount_curr - pay_curr) > 0.001 THEN 1 
                        WHEN (now()::date - transaction_date) BETWEEN 46 AND 60 
                            AND acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                            AND amount_curr - pay_curr <> 0 THEN 1 
                        ELSE 0 
                    END
                ) AS sixty_count,
                SUM(
                    CASE 
                        WHEN (now()::date - transaction_date) BETWEEN 61 AND 90 
                            AND acc_type IN ('REC', 'CTDS') 
                            AND ABS(amount_curr - pay_curr) > 0.001 THEN 1 
                        WHEN (now()::date - transaction_date) BETWEEN 61 AND 90 
                            AND acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                            AND amount_curr - pay_curr <> 0 THEN 1 
                        ELSE 0 
                    END
                ) AS ninety_count,
                SUM(
                    CASE 
                        WHEN (now()::date - transaction_date) BETWEEN 91 AND 180 
                            AND acc_type IN ('REC', 'CTDS') 
                            AND ABS(amount_curr - pay_curr) > 0.001 THEN 1 
                        WHEN (now()::date - transaction_date) BETWEEN 91 AND 180 
                            AND acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                            AND amount_curr - pay_curr <> 0 THEN 1 
                        ELSE 0 
                    END
                ) AS one_eighty_count,
                SUM(
                    CASE 
                        WHEN (now()::date - transaction_date) > 180
                            AND acc_type IN ('REC', 'CTDS') 
                            AND ABS(amount_curr - pay_curr) > 0.001 THEN 1 
                        WHEN (now()::date - transaction_date) > 180
                            AND acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                            AND amount_curr - pay_curr <> 0 THEN 1 
                        ELSE 0 
                    END
                ) AS one_eighty_plus_count,
                SUM(
                    CASE 
                        WHEN (now()::date - transaction_date) BETWEEN 181 AND 365
                            AND acc_type IN ('REC', 'CTDS') 
                            AND ABS(amount_curr - pay_curr) > 0.001 THEN 1 
                        WHEN (now()::date - transaction_date) BETWEEN 181 AND 365 
                            AND acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                            AND amount_curr - pay_curr <> 0 THEN 1 
                        ELSE 0 
                    END
                ) AS three_sixty_five_count,
                SUM(
                    CASE 
                        WHEN (now()::date - transaction_date) > 365
                            AND acc_type IN ('REC', 'CTDS') 
                            AND ABS(amount_curr - pay_curr) > 0.001 THEN 1 
                        WHEN (now()::date - transaction_date) > 365
                            AND acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') 
                            AND amount_curr - pay_curr <> 0 THEN 1 
                        ELSE 0 
                    END
                ) AS three_sixty_five_plus_count
            FROM
                account_utilizations
            WHERE
                acc_mode = 'AR'
                AND transaction_date IS NOT NULL
                AND document_status = 'FINAL'
                AND organization_id IS NOT NULL
                AND entity_code = :entityCode
                AND (:orgId IS NULL OR organization_id = :orgId::uuid)
                AND acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC')
                AND deleted_at IS NULL
            GROUP BY
                organization_id, entity_code, currency
        """
    )
    suspend fun getInvoicesOnAccountAgeingBucket(entityCode: Int, orgId: String?): List<CustomerOutstandingAgeing>
    @NewSpan
    @Query(
        """
        SELECT  COUNT(distinct trade_party_mapping_id) FROM account_utilizations
        WHERE acc_mode = 'AP' AND acc_type IN ('PINV','PREIMB') AND deleted_at IS NULL AND migrated = false AND amount_curr > pay_curr AND
        CASE WHEN :entity IS NOT NULL THEN entity_code = :entity ELSE TRUE END
    """
    )
    suspend fun getOrganizationCount(entity: Int?): Long?

    @NewSpan
    @Query(
        """ 
        SELECT
        count(*)
        FROM (
            SELECT
                au.acc_code,
                p.sage_ref_number,
                au.acc_mode,
                au.amount_curr AS payment_amount,
                au.amount_loc,
                au.pay_curr AS utilized_amount,
                au.pay_loc AS payment_loc,
                au.created_at,
                au.currency,
                au.entity_code,
                au.led_currency AS ledger_currency,
                au.organization_name,
                au.document_no,
                au.document_value AS payment_number,
                au.sign_flag,
                au.transaction_date,
                au.updated_at,
                (
                    CASE WHEN au.pay_curr = 0 THEN
                        'UNUTILIZED'
                    WHEN (au.amount_curr - au.pay_curr) > 0 THEN
                        'PARTIAL_UTILIZED'
                    ELSE
                        'UTILIZED'
                    END
                ) utilization_status
            FROM
                account_utilizations au
		        JOIN payments p ON au.document_value = p.payment_num_value
            WHERE (:query IS NULL OR au.document_value LIKE :query 
                    OR p.sage_ref_number LIKE :query)
                AND au.organization_id = :organizationId
                AND au.acc_type = 'REC'
                AND au.entity_code = :entityCode
        ) subquery
        WHERE
            utilization_status::varchar IN (:statusList)
        """
    )
    suspend fun getCount(organizationId: UUID, statusList: List<DocStatus>?, query: String?, entityCode: Int): Long

    @NewSpan
    @Query(
        """
        WITH FILTERS AS (
            SELECT id 
            FROM account_utilizations
            WHERE amount_curr <> 0 
                AND case when acc_type in ('SINV', 'SCN', 'PINV', 'PCN', 'PAY', 'REC', 'VTDS', 'CTDS') THEN (amount_curr - pay_curr) > 1 ELSE (amount_curr - pay_curr) > 0 END 
                AND organization_id in (:orgId)
                AND document_status = 'FINAL'
                AND ((:accType) is null or acc_type::varchar in (:accType))
                AND (:entityCode is null OR entity_code = :entityCode)
                AND (:startDate is null OR transaction_date >= :startDate::date)
                AND (:endDate is null OR transaction_date <= :endDate::date)
                AND document_value ilike :query
                AND ((:accMode) is null or acc_mode::varchar in (:accMode))
                AND document_status != 'DELETED'::document_status
                AND deleted_at is null
                AND settlement_enabled = true
            ORDER BY transaction_date DESC, id
            LIMIT :limit
            OFFSET :offset
        )
        SELECT 
            au.id,
            s.source_id,
            s.source_type,
            COALESCE(s.amount, 0) as settled_tds,
            s.currency as tds_currency,
            au.organization_id,
            au.trade_party_mapping_id as mapping_id,
            document_no, 
            document_value, 
            acc_type as document_type,
            acc_type as account_type,
            au.transaction_date as document_date,
            due_date, 
            COALESCE(amount_curr, 0) as document_amount, 
            COALESCE(amount_loc, 0) as document_led_amount, 
            COALESCE(amount_loc - pay_loc, 0) as document_led_balance,
            COALESCE(taxable_amount, 0) as taxable_amount,  
            COALESCE(amount_curr, 0) as after_tds_amount, 
            COALESCE(pay_curr, 0) as settled_amount, 
            COALESCE(amount_curr - pay_curr, 0) as balance_amount,
            COALESCE(
                CASE 
                WHEN au.acc_mode = 'AP' AND au.created_at < '2023-07-28' THEN 0 
                ELSE tds_amount
                END, 0
            ) as tds,
            au.currency, 
            au.led_currency, 
            au.sign_flag,
            au.acc_mode,
            au.migrated,
            true as approved,
            COALESCE(
                CASE WHEN 
                    (p.exchange_rate is not null) 
                    THEN p.exchange_rate 
                    ELSE ((case when amount_curr != 0 then amount_loc / amount_curr else 1 END)) 
                    END,
                 1) AS exchange_rate
            FROM account_utilizations au
            LEFT JOIN payments p ON 
                p.payment_num = au.document_no
            LEFT JOIN settlements s ON 
                s.destination_id = au.document_no 
                AND s.destination_type::varchar = au.acc_type::varchar 
                AND s.source_type::varchar in ('CTDS','VTDS')
            WHERE au.id in (
                SELECT id from FILTERS
            )
            AND au.deleted_at is null
            AND au.document_status != 'DELETED'::document_status
            AND s.deleted_at is null
            AND p.deleted_at is null 
            AND au.is_void = false
            AND au.settlement_enabled = true
            AND 
            (
                :documentPaymentStatus is null OR 
                (
                    CASE 
                        WHEN :documentPaymentStatus = 'partial_paid' THEN amount_curr - pay_curr > 0
                        WHEN :documentPaymentStatus = 'unpaid' THEN pay_curr = 0
                        WHEN :documentPaymentStatus = 'partially_utilized' THEN amount_curr - pay_curr > 0
                        WHEN :documentPaymentStatus = 'unutilized' THEN pay_curr = 0
                    END
                )
            )
            ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                    CASE 
                         WHEN :sortBy = 'transactionDate' THEN EXTRACT(epoch FROM au.transaction_date)::numeric
                         WHEN :sortBy ='dueDate' THEN EXTRACT(epoch FROM au.due_date)::numeric
                         WHEN :sortBy = 'documentAmount' then au.amount_curr
                         WHEN :sortBy = 'paidAmount' then au.pay_curr
                         when :sortBy = 'balanceAmount' then COALESCE(au.amount_curr - au.pay_curr, 0)
                         when :sortBy = 'tdsAmount' then au.tds_amount
                         when :sortBy = 'settledTds' then s.amount
                         
                    END  
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN EXTRACT(epoch FROM au.transaction_date)::numeric
                         WHEN :sortBy ='dueDate' THEN EXTRACT(epoch FROM au.due_date)::numeric
                         WHEN :sortBy = 'documentAmount' then au.amount_curr
                         WHEN :sortBy = 'paidAmount' then au.pay_curr
                         when :sortBy = 'balanceAmount' then COALESCE(au.amount_curr - au.pay_curr, 0)
                         when :sortBy = 'tdsAmount' then au.tds_amount
                         when :sortBy = 'settledTds' then s.amount
                    END       
            END 
            Asc
        """
    )
    suspend fun getDocumentList(
        limit: Int? = null,
        offset: Int? = null,
        accType: List<AccountType>?,
        orgId: List<UUID>,
        entityCode: Int?,
        startDate: Timestamp?,
        endDate: Timestamp?,
        query: String?,
        accMode: List<String>?,
        sortBy: String?,
        sortType: String?,
        documentPaymentStatus: String?
    ): List<Document?>

    @NewSpan
    @Query(
        """
        SELECT 
            count(id)
                FROM account_utilizations
                WHERE 
                    amount_curr <> 0 
                    AND case when acc_type in ('SINV', 'SCN', 'PINV', 'PCN', 'PAY', 'REC', 'VTDS', 'CTDS') THEN (amount_curr - pay_curr) > 1 ELSE (amount_curr - pay_curr) > 0 END
                    AND document_status = 'FINAL'
                    AND organization_id in (:orgId)
                    AND ((:accType) is null or acc_type::varchar in (:accType))
                    AND (:entityCode is null OR entity_code = :entityCode)
                    AND (:startDate is null OR transaction_date >= :startDate::date)
                    AND (:endDate is null OR transaction_date <= :endDate::date)
                    AND document_value ilike :query
                    AND deleted_at is null  and is_void = false
                    AND document_status != 'DELETED'::document_status
                    AND settlement_enabled = true
                    AND 
                    (
                        :documentPaymentStatus is null OR 
                        (
                            CASE 
                                WHEN :documentPaymentStatus = 'partial_paid' THEN amount_curr - pay_curr > 0
                                WHEN :documentPaymentStatus = 'unpaid' THEN pay_curr = 0
                                WHEN :documentPaymentStatus = 'partially_utilized' THEN amount_curr - pay_curr > 0
                                WHEN :documentPaymentStatus = 'unutilized' THEN pay_curr = 0
                            END
                        )
            )
    """
    )
    suspend fun getDocumentCount(accType: List<AccountType>?, orgId: List<UUID>, entityCode: Int?, startDate: Timestamp?, endDate: Timestamp?, query: String?, documentPaymentStatus: String?): Long?

    @NewSpan
    @Query(
        """
            UPDATE
                account_utilizations
            SET 
                document_value = :newDocValue,
                document_no = :newDocNo
            WHERE
                document_value = :docValue
                AND amount_curr = :amount
                AND transaction_date = :transactionDate::DATE
                AND organization_id = :organizationId
                AND tagged_organization_id = :taggedOrganizationId
                AND acc_type = :accType::account_type
        """
    )
    suspend fun updateAccountUtilizationForPayment(
        docValue: String,
        amount: BigDecimal,
        transactionDate: Date,
        organizationId: UUID,
        taggedOrganizationId: UUID,
        accType: String,
        newDocValue: String,
        newDocNo: Long
    ): Int

    @Query(
        """
             SELECT
                document_no,
                document_value,
                led_currency,
                amount_loc,
                pay_loc,
                due_date,
                CASE
                    WHEN acc_type = 'SINV' THEN 'INVOICE'
                    WHEN acc_type = 'SCN' THEN 'CREDIT_NOTE'
                    ELSE NULL
                END AS invoice_type,
                CASE
                    WHEN (CURRENT_DATE - due_date) BETWEEN 0 AND 30 THEN '0-30 days'
                    WHEN (CURRENT_DATE - due_date) BETWEEN 31 AND 60 THEN '31-60 days'
                    WHEN (CURRENT_DATE - due_date) BETWEEN 61 AND 90 THEN '61-90 days'
                    WHEN (CURRENT_DATE - due_date) BETWEEN 91 AND 180 THEN '91-180 days'
                    WHEN (CURRENT_DATE - due_date) BETWEEN 181 AND 365 THEN '181-365 days'
                    WHEN (CURRENT_DATE - due_date) > 365 THEN '365+ days'
                END AS relative_duration
            FROM
                account_utilizations
            WHERE
                organization_id::uuid = :tradePartyDetailId
                AND acc_mode = 'AR'
                AND acc_type IN ('SINV', 'SCN')
                AND document_status = 'FINAL'
                AND deleted_at IS NULL
                AND amount_loc - pay_loc NOT BETWEEN -1 AND 1
                AND entity_code = :entityCode
                AND (
                    :transactionDateStart IS NULL
                    OR :transactionDateEnd IS NULL
                    OR (transaction_date::DATE BETWEEN CAST(:transactionDateStart AS DATE) AND CAST(:transactionDateEnd AS DATE))
                )
            ORDER BY
                CASE
                    WHEN :sortType = 'ASC' THEN transaction_date
                    ELSE transaction_date END DESC
            LIMIT :limit
        """
    )
    suspend fun getInvoicesForDunning(
        entityCode: Int,
        tradePartyDetailId: UUID,
        limit: Int? = 1000,
        transactionDateStart: Date? = null,
        transactionDateEnd: Date? = null,
        sortType: String? = "ASC"
    ): List<DunningInvoices>

    @Query(
        """
            SELECT  document_value, led_currency, amount_loc, pay_loc,transaction_date::TIMESTAMP,sign_flag
            FROM account_utilizations WHERE 
            organization_id::uuid = :tradePartyDetailId
            AND entity_code = :entityCode
            AND acc_mode = 'AR' AND acc_type in ('PAY','REC','MISC','OPDIV','BANK','MTCCV') AND document_status = 'FINAL'
            AND deleted_at IS NULL
            AND amount_loc - pay_loc > 0
            ORDER BY transaction_date DESC
            LIMIT 100
        """
    )
    suspend fun getPaymentsForDunning(entityCode: Int, tradePartyDetailId: UUID): List<DunningPayments>

    @NewSpan
    @Query(
        """
                WITH outstanding_data AS (
                            SELECT
                                organization_id as trade_party_detail_id,
                                entity_code,
                                led_currency,
                                max(organization_name) as trade_party_detail_name,
                                max(id) as id,
                                max(tagged_organization_id :: VARCHAR) as tagged_organization_id,
                                (
                                    CASE
                                        WHEN :ageingStartDay != :ageingLastDay THEN SUM(
                                            CASE
                                                WHEN acc_type :: varchar IN ('SINV', 'SCN', 'SREIMB', 'SREIMBCN')
                                                and(now() :: date - due_date) BETWEEN :ageingStartDay
                                                AND :ageingLastDay THEN sign_flag * (amount_loc - pay_loc)
                                                ELSE 0
                                            END
                                        )
                                        ELSE SUM(
                                            CASE
                                                WHEN acc_type :: varchar IN ('SINV', 'SCN', 'SREIMB', 'SREIMBCN')
                                                and(now() :: date - due_date) >= :ageingStartDay THEN sign_flag * (amount_loc - pay_loc)
                                                ELSE 0
                                            END
                                        )
                                    END
                                ) as outstanding_amount,
                                (
                                    CASE
                                        WHEN :ageingStartDay != :ageingLastDay THEN sum(
                                            CASE
                                                WHEN acc_type :: varchar IN ('REC', 'CTDS', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC', 'PAY')
                                                and(now() :: date - transaction_date) BETWEEN :ageingStartDay
                                                AND :ageingLastDay THEN sign_flag * (amount_loc - pay_loc)
                                                ELSE 0
                                            END
                                        )
                                        ELSE sum(
                                            CASE
                                                WHEN acc_type in ('REC', 'CTDS', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC', 'PAY')
                                                    and(now() :: date - transaction_date) >= :ageingStartDay
                                                THEN sign_flag * (amount_loc - pay_loc)
                                                ELSE 0
                                            END
                                        )
                                    END
                                ) as on_account_amount
                            from
                                account_utilizations
                            WHERE
                                acc_mode = 'AR'
                                AND (COALESCE(:taggedOrganizationIds) IS NULL OR tagged_organization_id::UUID IN (:taggedOrganizationIds))
                                AND (COALESCE(:exceptionTradePartyDetailId) IS NULL OR organization_id::UUID NOT IN (:exceptionTradePartyDetailId))
                                AND transaction_date IS NOT NULL
                                AND document_status = 'FINAL'
                                AND organization_id IS NOT NULL
                                AND amount_loc - pay_loc > 0
                                AND entity_code = :entityCode
                                AND (:query IS NULL OR LOWER(organization_name) ILIKE :query)
                                AND acc_type in ('SINV','SCN','SREIMB','SREIMBCN','REC', 'CTDS', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC', 'PAY')
                                AND deleted_at IS NULL 
                            GROUP BY
                                organization_id,
                                entity_code,
                                led_currency
                )
                
                select
                        trade_party_detail_id,
                        entity_code,
                        led_currency,
                        trade_party_detail_name,
                        id,
                        tagged_organization_id,
                        ( outstanding_amount + on_account_amount ) AS outstanding_amount,
                        on_account_amount
                FROM
                        outstanding_data
                WHERE
                    (COALESCE(:totalDueOutstanding) IS NULL OR (outstanding_amount + on_account_amount) > :totalDueOutstanding)
                OFFSET 
                    GREATEST(0, ((:pageIndex - 1) * :pageSize))
                LIMIT 
                    :pageSize
            """
    )
    suspend fun listOnAccountAndOutstandingBasedOnDunningCycleFilters(
        query: String?,
        totalDueOutstanding: BigDecimal? = null,
        entityCode: Int,
        ageingStartDay: Int,
        ageingLastDay: Int,
        taggedOrganizationIds: List<UUID>?,
        exceptionTradePartyDetailId: List<UUID>?,
        sortBy: String? = "created_at",
        sortType: String? = "ASC",
        pageIndex: Int,
        pageSize: Int
    ): List<CustomerOutstandingAndOnAccountResponse>

    @NewSpan
    @Query(
        """
                WITH a AS (
                            SELECT
                                organization_id as trade_party_detail_id,
                                entity_code,
                                led_currency,
                                max(organization_name) as trade_party_detail_name,
                                max(id) as id,
                                max(tagged_organization_id :: VARCHAR) as tagged_organization_id,
                                (
                                    CASE
                                        WHEN :ageingStartDay != :ageingLastDay THEN SUM(
                                            CASE
                                                WHEN acc_type :: varchar IN ('SINV', 'SCN', 'SREIMB', 'SREIMBCN')
                                                and(now() :: date - due_date) BETWEEN :ageingStartDay
                                                AND :ageingLastDay THEN sign_flag * (amount_loc - pay_loc)
                                                ELSE 0
                                            END
                                        )
                                        ELSE SUM(
                                            CASE
                                                WHEN acc_type :: varchar IN ('SINV', 'SCN', 'SREIMB', 'SREIMBCN')
                                                and(now() :: date - due_date) > :ageingStartDay THEN sign_flag * (amount_loc - pay_loc)
                                                ELSE 0
                                            END
                                        )
                                    END
                                ) as outstanding_amount,
                                (
                                    CASE
                                        WHEN :ageingStartDay != :ageingLastDay THEN sum(
                                            CASE
                                                WHEN acc_type :: varchar IN ('REC', 'CTDS', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC', 'PAY')
                                                and(now() :: date - transaction_date) BETWEEN :ageingStartDay
                                                AND :ageingLastDay THEN sign_flag * (amount_loc - pay_loc)
                                                ELSE 0
                                            END
                                        )
                                        ELSE sum(
                                            CASE
                                                WHEN acc_type in ('REC', 'CTDS', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC', 'PAY')
                                                    and(now() :: date - transaction_date) >= :ageingStartDay
                                                THEN sign_flag * (amount_loc - pay_loc)
                                                ELSE 0
                                            END
                                        )
                                    END
                                ) as on_account_amount
                            from
                                account_utilizations
                            WHERE
                                acc_mode = 'AR'
                                AND (COALESCE(:taggedOrganizationIds) IS NULL OR tagged_organization_id::UUID IN (:taggedOrganizationIds))
                                AND (COALESCE(:exceptionTradePartyDetailId) IS NULL OR organization_id::UUID NOT IN (:exceptionTradePartyDetailId))
                                AND transaction_date IS NOT NULL
                                AND document_status = 'FINAL'
                                AND organization_id IS NOT NULL
                                AND amount_loc - pay_loc > 0
                                AND entity_code = :entityCode
                                AND (:query IS NULL OR LOWER(organization_name) ILIKE :query)
                                AND acc_type in ('SINV','SCN','SREIMB','SREIMBCN','REC', 'CTDS', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC', 'PAY')
                                AND deleted_at IS NULL 
                            GROUP BY
                                organization_id,
                                entity_code,
                                led_currency
                )
                
                SELECT
                        count(*)
                FROM
                        a
                WHERE
                    (COALESCE(:totalDueOutstanding) IS NULL OR (outstanding_amount + on_account_amount) > :totalDueOutstanding)
            """
    )
    suspend fun countOnAccountAndOutstandingBasedOnDunningCycleFilters(
        query: String?,
        totalDueOutstanding: BigDecimal? = null,
        entityCode: Int,
        ageingStartDay: Int,
        ageingLastDay: Int,
        taggedOrganizationIds: List<UUID?>?,
        exceptionTradePartyDetailId: List<UUID?>?
    ): Long
    @NewSpan
    @Query(
        """
            SELECT
            led_currency as ledger_currency,
            sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-01-01')::DATE
			    AND CONCAT(:year, '-01-31')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS january,
	        sum(
                CASE WHEN transaction_date BETWEEN CONCAT(:year, '-02-01')::DATE
			    AND CONCAT(:year, CASE WHEN :isLeapYear = TRUE THEN '-02-29' ELSE '-02-28' END)::DATE THEN sign_flag * amount_loc ELSE 0 END) AS february,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-03-01')::DATE
			    AND CONCAT(:year, '-03-31')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS march,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-04-01')::DATE
			    AND CONCAT(:year, '-04-30')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS april,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-05-01')::DATE
			    AND CONCAT(:year, '-05-31')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS may,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-06-01')::DATE
			    AND CONCAT(:year, '-06-30')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS june,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-07-01')::DATE
			    AND CONCAT(:year, '-07-31')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS july,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-08-01')::DATE
			    AND CONCAT(:year, '-08-31')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS august,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-09-01')::DATE
			    AND CONCAT(:year, '-09-30')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS september,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-10-01')::DATE
			    AND CONCAT(:year, '-10-31')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS october,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-11-01')::DATE
			    AND CONCAT(:year, '-11-30')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS november,
	        sum(
		        CASE WHEN transaction_date BETWEEN CONCAT(:year, '-12-01')::DATE
			    AND CONCAT(:year, '-12-31')::DATE THEN sign_flag * amount_loc ELSE 0 END) AS december
            FROM
	            account_utilizations
            WHERE
                acc_mode = 'AR'
                AND organization_id = :orgId::UUID
                AND acc_type IN ('REC','BANK','MISC')
                AND acc_code = 223000
                AND document_status = 'FINAL'
                AND deleted_at IS NULL
                AND entity_code = :entityCode
            group by organization_id, led_currency
        """
    )
    suspend fun getCustomerMonthlyPayment(orgId: String, year: String, isLeapYear: Boolean, entityCode: Int): CustomerMonthlyPayment?

    @NewSpan
    @Query(
        """
            select organization_id::varchar,
            sum(case when acc_type in ('SINV', 'SREIMB', 'SCN', 'SREIMBCN') and amount_curr - pay_curr <> 0 and document_status = 'FINAL' then 1 else 0 end) as open_invoices_count,
            sum(case when acc_type in ('SINV', 'SREIMB', 'SCN', 'SREIMBCN') and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc)  else 0 end) as open_invoices_led_amount,
            sum(case when acc_type in ('SINV', 'SREIMB', 'SCN', 'SREIMBCN') and document_status = 'FINAL' AND due_date < now()::date then sign_flag * (amount_loc - pay_loc) else 0 end) as overdue_open_invoices_led_amount,
            sum(case when acc_type in ('SINV', 'SREIMB', 'SCN', 'SREIMBCN', 'REC', 'CTDS', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'PAY') and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) as outstanding_led_amount
            from account_utilizations
            where acc_type in ('SINV','SCN','REC', 'CTDS', 'SREIMB', 'SREIMBCN', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC', 'PAY') and acc_mode = 'AR' and document_status = 'FINAL'  
            and organization_id IN (:orgIds) and entity_code IN (:entityCodes) and deleted_at is null
            group by organization_id
        """
    )
    suspend fun getTradePartyOutstanding(orgIds: List<UUID>, entityCodes: List<Int>): List<TradePartyOutstandingRes>?

    @NewSpan
    @Query(
        """
                SELECT organization_id::VARCHAR, currency,led_currency , sign_flag, amount_curr, pay_curr,amount_loc, pay_loc, transaction_date,due_date,entity_code
                FROM account_utilizations 
                WHERE acc_type::varchar in (:accType)
                AND acc_mode = 'AP'
                AND document_status in ('FINAL', 'PROFORMA')
                AND organization_id IS NOT NULL
                AND due_date IS NOT NULL
                AND organization_id = :orgId::uuid AND deleted_at IS NULL AND is_void = false
                AND (:entityCode IS NULL OR entity_code = :entityCode)
                AND (:startDate is null or :endDate is null or transaction_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
                AND
                (
                    CASE 
                        WHEN acc_type IN ('PINV','PREIMB','PCN') THEN abs(amount_loc - pay_loc) > 0
                        ELSE amount_loc > 0
                    END
                )
            """
    )
    suspend fun getDocumentsForLSP(orgId: String, entityCode: Int?, startDate: String?, endDate: String?, accType: List<String>): List<DocumentResponse?>

    @Query(
        """
        SELECT
            organization_id,
            transaction_date,
            service_type,
            document_value,
            document_no,
            CASE WHEN acc_type IN ('PINV','PREIMB','PCN') THEN sign_flag * amount_loc ELSE 0 END as debit,
            CASE WHEN acc_type IN ('PAY','MISC','BANK') AND acc_code = 321000 THEN sign_flag * amount_loc ELSE 0 END as credit,
            led_currency as ledger_currency,
            acc_type::text as type
        FROM
            account_utilizations
        WHERE
            document_status IN ('FINAL','PROFORMA')
            AND organization_id IS NOT NULL
            AND acc_mode = 'AP'
            AND organization_id = :orgId::uuid
            AND (:entityCode IS NULL OR entity_code = :entityCode) 
            AND EXTRACT(YEAR FROM transaction_date) = :year
            AND EXTRACT(MONTH FROM transaction_date) = :month
            AND acc_type::varchar IN (:accTypes)
            AND deleted_at IS NULL
            AND is_void = false
        ORDER BY transaction_date, document_no
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageLimit))
        LIMIT :pageLimit
    """
    )
    suspend fun getLedgerForLSP(orgId: String, entityCode: Int?, year: Int, month: Int, accTypes: List<String>, pageIndex: Int?, pageLimit: Int?): List<LedgerDetails>?

    @Query(
        """
        SELECT
            COALESCE(COUNT(*), 0)
        FROM
            account_utilizations
        WHERE
            document_status = 'FINAL'
            AND organization_id IS NOT NULL
            AND acc_mode = 'AP'
            AND organization_id = :orgId::uuid
            AND (:entityCode IS NULL OR entity_code = :entityCode) 
            AND EXTRACT(YEAR FROM transaction_date) = :year
            AND EXTRACT(MONTH FROM transaction_date) = :month
            AND acc_type::varchar IN (:accTypes)
            AND deleted_at IS NULL
            AND is_void = false
    """
    )
    suspend fun getLedgerForLSPCount(orgId: String, entityCode: Int?, year: Int, month: Int, accTypes: List<String>): Long

    @NewSpan
    @Query(
        """
                WITH temp_table AS (
                  SELECT 
                    organization_id as trade_party_detail_id, 
                    entity_code, 
                    led_currency, 
                    max(organization_name) as trade_party_detail_name, 
                    max(id) as id, 
                    max(
                      tagged_organization_id :: VARCHAR
                    ) as tagged_organization_id, 
                    (
                      SUM(
                        CASE WHEN acc_type :: varchar IN (
                          'SINV', 'SCN', 'SREIMB', 'SREIMBCN'
                        ) THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END
                      )
                    ) as open_invoice_amount,
                    (
                      SUM(
                        CASE WHEN acc_type :: varchar IN (
                          'SINV', 'SCN', 'SREIMB', 'SREIMBCN', 'REC', 'CTDS'
                        ) THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END
                      )
                    ) as outstanding_amount, 
                    (
                      sum(
                        CASE WHEN acc_type :: varchar IN ('REC', 'CTDS') THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END
                      )
                    ) as on_account_amount 
                  from 
                    account_utilizations 
                  WHERE 
                    acc_mode = 'AR' 
                    AND (
                      COALESCE(:serviceTypes) IS NULL 
                      OR service_type :: VARCHAR IN (:serviceTypes)
                    ) 
                    AND transaction_date IS NOT NULL 
                    AND document_status = 'FINAL' 
                    AND organization_id IS NOT NULL 
                    AND amount_loc - pay_loc > 0 
                    AND (
                      COALESCE(:entityCode) IS NULL 
                      OR entity_code IN (:entityCode)
                    )
                    AND (
                      :query IS NULL 
                      OR LOWER(organization_name) ILIKE :query
                    ) 
                    AND acc_type in (
                      'REC', 'CTDS', 'SINV', 'SCN', 'SREIMB', 
                      'SREIMBCN'
                    ) 
                    AND deleted_at IS NULL 
                  GROUP BY 
                    organization_id, 
                    entity_code, 
                    led_currency
                ) 
                select 
                      tt.trade_party_detail_id, 
                      tt.entity_code, 
                      tt.led_currency, 
                      tt.trade_party_detail_name, 
                      tt.id, 
                      tt.tagged_organization_id, 
                      tt.open_invoice_amount, 
                      tt.on_account_amount, 
                      tt.outstanding_amount, 
                      os.organization_stakeholder_name 
                FROM 
                      temp_table tt 
                      left join organization_stakeholders os on os.organization_id :: varchar = tt.tagged_organization_id 
                      and os.organization_stakeholder_type = 'CREDIT_CONTROLLER'
                ORDER BY
                    CASE WHEN :sortBy = 'onAccountAmount'
                        THEN CASE WHEN :sortType = 'Asc' THEN on_account_amount END
                    END ASC,
                    CASE WHEN :sortBy = 'onAccountAmount'
                        THEN CASE WHEN :sortType = 'Desc' THEN on_account_amount END
                    END DESC,
                    CASE WHEN :sortBy = 'outstandingAmount'
                        THEN CASE WHEN :sortType = 'Asc' THEN outstanding_amount END
                    END ASC,
                    CASE WHEN :sortBy = 'outstandingAmount'
                        THEN CASE WHEN :sortType = 'Desc' THEN outstanding_amount END
                    END DESC
                OFFSET 
                    GREATEST(0, ((:pageIndex - 1) * :pageSize))
                LIMIT 
                    :pageSize
            """
    )
    suspend fun overallOutstandingAndOnAccountPerTradeParty(
        pageSize: Int? = 10,
        pageIndex: Int? = 1,
        entityCode: List<Int>?,
        query: String?,
        serviceTypes: List<String>?,
        sortBy: String? = "outstandingAmount",
        sortType: String? = "Desc"
    ): List<OverallOutstandingAndOnAccountResponse>

    @NewSpan
    @Query(
        """
                WITH temp_table AS (
                  SELECT 
                    organization_id as trade_party_detail_id, 
                    entity_code, 
                    led_currency, 
                    max(organization_name) as trade_party_detail_name, 
                    max(id) as id, 
                    max(
                      tagged_organization_id :: VARCHAR
                    ) as tagged_organization_id, 
                    (
                      SUM(
                        CASE WHEN acc_type :: varchar IN (
                          'SINV', 'SCN', 'SREIMB', 'SREIMBCN'
                        ) THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END
                      )
                    ) as open_invoice_amount, 
                    (
                      sum(
                        CASE WHEN acc_type :: varchar IN ('REC', 'CTDS') THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END
                      )
                    ) as on_account_amount 
                  from 
                    account_utilizations 
                  WHERE 
                    acc_mode = 'AR' 
                    AND (
                      COALESCE(:serviceTypes) IS NULL 
                      OR service_type :: VARCHAR IN (:serviceTypes)
                    ) 
                    AND transaction_date IS NOT NULL 
                    AND document_status = 'FINAL' 
                    AND organization_id IS NOT NULL 
                    AND amount_loc - pay_loc > 0 
                    AND (
                      COALESCE(:entityCode) IS NULL 
                      OR entity_code IN (:entityCode)
                    )
                    AND (
                      :query IS NULL 
                      OR LOWER(organization_name) ILIKE :query
                    ) 
                    AND acc_type in (
                      'REC', 'CTDS', 'SINV', 'SCN', 'SREIMB', 
                      'SREIMBCN'
                    ) 
                    AND deleted_at IS NULL 
                  GROUP BY 
                    organization_id, 
                    entity_code, 
                    led_currency
                ) 
                select 
                  count(tt.id)
                FROM 
                  temp_table tt 
                  left join organization_stakeholders os on os.organization_id :: varchar = tt.tagged_organization_id 
                  and os.organization_stakeholder_type = 'CREDIT_CONTROLLER'
            """
    )
    suspend fun countOverallOutstandingAndOnAccountPerTradeParty(
        entityCode: List<Int>?,
        query: String?,
        serviceTypes: List<String>?
    ): Long

    @Query(
        """
        SELECT
            TO_CHAR(transaction_date, 'Mon') AS month,
            TO_CHAR(transaction_date, 'YYYY') AS year,
            SUM(CASE WHEN acc_type :: varchar IN ('SINV', 'SCN', 'SREIMB', 'SREIMBCN') THEN sign_flag * (pay_loc) ELSE 0 END) as collected_amount,
            SUM(CASE WHEN acc_type :: varchar IN ('SINV', 'SCN', 'SREIMB', 'SREIMBCN') THEN sign_flag * (amount_loc) ELSE 0 END) as open_invoice_amount,
            SUM(CASE WHEN acc_type :: varchar IN ('SINV', 'SCN', 'SREIMB', 'SREIMBCN') THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END) as outstanding_amount
        FROM
            account_utilizations
        WHERE
            transaction_date >= DATE_TRUNC('MONTH', :timestamp::TIMESTAMP) - INTERVAL '11 months'
            AND transaction_date <= :timestamp::TIMESTAMP
            AND acc_mode = 'AR'
            AND transaction_date IS NOT NULL
            AND document_status = 'FINAL'
            AND organization_id IS NOT NULL
            AND amount_loc - pay_loc > 0
            AND acc_type IN ('REC', 'CTDS', 'SINV', 'SCN', 'SREIMB', 'SREIMBCN')
            AND deleted_at IS NULL
            AND (COALESCE(:serviceTypes) IS NULL OR service_type::VARCHAR IN (:serviceTypes))
            AND (COALESCE(:entityCodes) IS NULL OR entity_code IN (:entityCodes))
        GROUP BY
            TO_CHAR(transaction_date, 'Mon'),
            TO_CHAR(transaction_date, 'YYYY')
        ORDER BY
            MIN(transaction_date)
    """
    )
    suspend fun monthWiseStatisticsOfAccountUtilization(
        timestamp: Timestamp,
        serviceTypes: List<String>?,
        entityCodes: List<Int>?
    ): List<MonthWiseStatisticsOfAccountUtilizationResponse>

    @NewSpan
    @Query(
        """
     SELECT COUNT(DISTINCT organization_id) AS total_customers , 
        COALESCE(SUM(sign_flag * (amount_loc - pay_loc)),0) as  total_outstanding_amount
        FROM account_utilizations WHERE acc_mode = 'AR' AND document_status = 'FINAL' AND deleted_at IS NULL
        AND acc_type IN ('SINV', 'SCN','SREIMB', 'SREIMBCN')
        AND due_date < now()::date 
        AND due_date IS NOT NULL
        AND (COALESCE(:defaultedOrgIds) IS NULL OR organization_id::UUID NOT IN (:defaultedOrgIds))
        AND (COALESCE(:entityCode) IS NULL OR entity_code  IN (:entityCode))
    """
    )
    suspend fun getCustomerWithOutStanding(entityCode: List<Int>?, defaultedOrgIds: List<UUID>?): DunningCardData

    @NewSpan
    @Query(
        """
            select 
            null as id,
            organization_id,
            entity_code,
            led_currency,
            max(organization_name) as organization_name,
            acc_mode,
            sum(
                CASE WHEN (acc_type::varchar in (:invoiceAccType)
                    and(due_date >= now()::date)) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS invoice_not_due_amount,
            sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType)
                    and (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS invoice_today_amount,        
            sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType)
                    and(now()::date - due_date) BETWEEN 1 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS invoice_thirty_amount,
            sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType)
                    and(now()::date - due_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS invoice_sixty_amount,
            sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType)
                    and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS invoice_ninety_amount,
            sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType)
                    and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS invoice_one_eighty_amount,
            sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType)
                    and(now()::date - due_date) BETWEEN 181 AND 365  THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS invoice_three_sixty_five_amount,
            sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType)
                    and(now()::date - due_date) > 365 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS invoice_three_sixty_five_plus_amount,
            sum(
                CASE WHEN due_date >= now()::date AND acc_type::varchar in (:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_not_due_count,
            sum(
                CASE WHEN (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 AND acc_type::varchar in (:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_today_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 1 AND 30 AND acc_type::varchar in (:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_thirty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 31 AND 60 AND acc_type::varchar in (:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_sixty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 61 AND 90 AND acc_type::varchar in (:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_ninety_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 AND acc_type::varchar in(:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_one_eighty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 181 AND 365 AND acc_type::varchar in (:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_three_sixty_five_count,
            sum(
                CASE WHEN (now()::date - due_date) > 365 AND acc_type::varchar in(:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_three_sixty_five_plus_count,
            sum(
                CASE WHEN (acc_type::varchar in (:creditNoteAccType)) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_count,
            sum(
                CASE WHEN (acc_type::varchar in (:onAccountAccountType)
                    and(due_date >= now()::date)) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS on_account_not_due_amount,
            sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS on_account_today_amount,        
            sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - due_date) BETWEEN 1 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS on_account_thirty_amount,
            sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - due_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS on_account_sixty_amount,
            sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS on_account_ninety_amount,
            sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS on_account_one_eighty_amount,
            sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - due_date) BETWEEN 181 AND 365  THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS on_account_three_sixty_five_amount,
            sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - due_date) > 365 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS on_account_three_sixty_five_plus_amount,
            sum(
                CASE WHEN due_date >= now()::date AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_not_due_count,
            sum(
                CASE WHEN (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_today_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 1 AND 30 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_thirty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 31 AND 60 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_sixty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 61 AND 90 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_ninety_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_one_eighty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 181 AND 365 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_three_sixty_five_count,
            sum(
                CASE WHEN (now()::date - due_date) > 365 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_three_sixty_five_plus_count,
            sum(
                CASE WHEN (due_date >= now()::date) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS not_due_outstanding,
            sum(
                CASE WHEN (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS today_outstanding,        
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 1 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS thirty_outstanding,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS sixty_outstanding,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS ninety_outstanding,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS one_eighty_outstanding,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 181 AND 365  THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS three_sixty_five_outstanding,
            sum(
                CASE WHEN (now()::date - due_date) > 365 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS three_sixty_five_plus_outstanding,
            sum(
            CASE WHEN acc_type::varchar in (:invoiceAccType) THEN
                sign_flag * (amount_loc - pay_loc)
            ELSE
                0
            END) AS total_open_invoice_amount,
            sum(
                CASE WHEN acc_type::varchar in (:creditNoteAccType) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS total_credit_note_amount,
            sum(
            CASE WHEN acc_type::varchar in (:onAccountAccountType) THEN
                sign_flag * (amount_loc - pay_loc)
            ELSE
                0
            END) AS total_open_account_amount,
        sum(sign_flag * (amount_loc - pay_loc)) AS total_outstanding,
        SUM(
            CASE WHEN (acc_type::VARCHAR IN (:invoiceAccType) or acc_type::VARCHAR IN (:creditNoteAccType)) THEN
              sign_flag * amount_loc
            ELSE
              0
            END
          ) AS total_invoice_amount,
          SUM(
            CASE WHEN acc_type::VARCHAR IN (:onAccountAccountType) THEN
              sign_flag * amount_loc
            ELSE
              0
            END
          ) AS total_on_account_amount,
          SUM(
            CASE WHEN acc_type::VARCHAR IN (:onAccountAccountType) THEN
              1
            ELSE
              0
            END
          ) AS total_on_account_count,
          SUM(
            CASE WHEN (acc_type::VARCHAR IN (:invoiceAccType) or acc_type::VARCHAR IN (:creditNoteAccType)) THEN
              1
            ELSE
              0
            END
          ) AS total_invoices_count,
        CURRENT_DATE AS created_at
    FROM 
        account_utilizations 
    WHERE 
        acc_mode::VARCHAR = :accMode 
        AND deleted_at IS NULL
        AND acc_type::VARCHAR IN (:accTypes)
        AND document_status IN ('FINAL')
        AND created_at >= '2023-07-28'
    GROUP BY 
        organization_id, entity_code, led_currency, acc_mode
        """
    )
    suspend fun getLedgerSummaryForAp(
        accTypes: List<String>,
        accMode: String,
        invoiceAccType: List<String>,
        onAccountAccountType: List<String>,
        creditNoteAccType: List<String>
    ): List<LedgerSummary>?
}
