package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.CustomerOutstandingAgeing
import com.cogoport.ares.api.payment.entity.OrgOutstanding
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentResponse
import com.cogoport.ares.api.settlement.entity.Document
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
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
        """select account_utilizations.id,document_no,document_value , zone_code,service_type,document_status,entity_code , category,org_serial_id,sage_organization_id
           ,organization_id, tagged_organization_id, trade_party_mapping_id, organization_name,acc_code,acc_type,account_utilizations.acc_mode,sign_flag,currency,led_currency,amount_curr, amount_loc,pay_curr
           ,pay_loc,due_date,transaction_date,created_at,updated_at, taxable_amount, migrated, is_void,tagged_bill_id,  tds_amount, tds_amount_loc
            from account_utilizations 
            where document_no in (:documentNo) and acc_type::varchar in (:accType) 
            and (:accMode is null or acc_mode=:accMode::account_mode)
             and account_utilizations.deleted_at is null order by updated_at desc"""
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
                AND au.acc_type = 'REC'
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
                count(c.organization_id)
            FROM (
                SELECT
                    organization_id
                FROM
                    account_utilizations
                WHERE
                    organization_name ILIKE :queryName || '%'
                    AND acc_mode = 'AR'
                    AND due_date IS NOT NULL
                    AND document_status in('FINAL')
                    AND organization_id IS NOT NULL
                    AND acc_type = 'SINV'
                    AND deleted_at IS NULL AND is_void IS false
                GROUP BY
                    organization_id) AS c
        """
    )
    suspend fun getInvoicesOutstandingAgeingBucketCount(queryName: String?, orgId: String?): Int

    @NewSpan
    @Query(
        """
            select organization_id::varchar, currency,
            sum(case when acc_type = 'SINV' and amount_curr - pay_curr <> 0 and document_status = 'FINAL' then 1 else 0 end) as open_invoices_count,
            sum(case when acc_type = 'SINV' and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end) as open_invoices_amount,
            sum(case when acc_type = 'SINV' and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) as open_invoices_led_amount,
            sum(case when acc_type = 'REC' and document_status = 'FINAL' and amount_curr - pay_curr <> 0 then 1 else 0 end) as payments_count,
            sum(case when acc_type = 'REC' and document_status = 'FINAL' then  amount_curr - pay_curr else 0 end) as payments_amount,
            sum(case when acc_type = 'REC' and document_status = 'FINAL' then  amount_loc - pay_loc else 0 end) as payments_led_amount,
            sum(case when acc_type = 'SINV' and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end) + sum(case when acc_type = 'REC' and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end) + sum(case when acc_type = 'SCN' and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end)as outstanding_amount,
            sum(case when acc_type =  'SINV' then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'REC' and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'SCN' and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) as outstanding_led_amount
            from account_utilizations
            where acc_type in ('SINV','SCN','REC') and acc_mode = 'AR' and document_status = 'FINAL' 
            and organization_id = :orgId::uuid and entity_code = :entityCode and deleted_at is null
            group by organization_id, currency
        """
    )
    suspend fun generateCustomerOutstanding(orgId: String, entityCode: Int): List<OrgOutstanding>

    @NewSpan
    @Query(
        """
            SELECT
                organization_id,
                entity_code,
                currency,
                max(organization_name) as organization_name,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(due_date >= now()::date) THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS not_due_led_amount,      
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 0 AND 30 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS thirty_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 31 AND 45 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS forty_five_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 46 AND 60 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS sixty_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS ninety_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS one_eighty_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) > 180 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS one_eighty_plus_led_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS total_led_outstanding,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(due_date >= now()::date) THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS not_due_curr_amount,      
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 0 AND 30 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS thirty_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 31 AND 45 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS forty_five_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 46 AND 60 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS sixty_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS ninety_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS one_eighty_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type
                        and(now()::date - due_date) > 180 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS one_eighty_plus_curr_amount,
                sum(
                    CASE WHEN acc_type = :accType::account_type THEN
                        amount_curr - pay_curr
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

    @NewSpan
    @Query(
        """
            SELECT
                organization_id,
                entity_code,
                currency,
                max(organization_name) as organization_name,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(transaction_date >= now()::date) THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS not_due_led_amount,      
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 0 AND 30 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS thirty_led_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 31 AND 45 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS forty_five_led_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 46 AND 60 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS sixty_led_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 61 AND 90 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS ninety_led_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 91 AND 180 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS one_eighty_led_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) > 180 THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS one_eighty_plus_led_amount,
                sum(
                    CASE WHEN acc_type = 'REC' THEN
                        amount_loc - pay_loc
                    ELSE
                        0
                    END) AS total_led_outstanding,
                sum(
                    CASE WHEN (acc_type = 'REC'
                        and(transaction_date >= now()::date)) THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS not_due_curr_amount,      
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 0 AND 30 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS thirty_curr_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 31 AND 45 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS forty_five_curr_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 46 AND 60 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS sixty_curr_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 61 AND 90 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS ninety_curr_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) BETWEEN 91 AND 180 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS one_eighty_curr_amount,
                sum(
                    CASE WHEN acc_type = 'REC'
                        and(now()::date - transaction_date) > 180 THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS one_eighty_plus_curr_amount,
                sum(
                    CASE WHEN acc_type = 'REC' THEN
                        amount_curr - pay_curr
                    ELSE
                        0
                    END) AS total_curr_outstanding,
                sum(
                    CASE WHEN transaction_date >= now()::date AND acc_type = 'REC' THEN
                        1
                    ELSE
                        0
                    END) AS not_due_count,
                sum(
                    CASE WHEN (now()::date - transaction_date) BETWEEN 0 AND 30 AND acc_type = 'REC' THEN
                        1
                    ELSE
                        0
                    END) AS thirty_count,
                sum(
                    CASE WHEN (now()::date - transaction_date) BETWEEN 31 AND 45 AND acc_type = 'REC' THEN
                        1
                    ELSE
                        0
                    END) AS forty_five_count,
                sum(
                    CASE WHEN (now()::date - transaction_date) BETWEEN 46 AND 60 AND acc_type = 'REC' THEN
                        1
                    ELSE
                        0
                    END) AS sixty_count,
                sum(
                    CASE WHEN (now()::date - transaction_date) BETWEEN 61 AND 90 AND acc_type = 'REC' THEN
                        1
                    ELSE
                        0
                    END) AS ninety_count,
                sum(
                    CASE WHEN (now()::date - transaction_date) BETWEEN 91 AND 180 AND acc_type = 'REC' THEN
                        1
                    ELSE
                        0
                    END) AS one_eighty_count,
                sum(
                    CASE WHEN (now()::date - transaction_date) > 180 AND acc_type = 'REC' THEN
                        1
                    ELSE
                        0
                    END) AS one_eighty_plus_count
            FROM
                account_utilizations
            WHERE
                acc_mode = 'AR'
                AND transaction_date IS NOT NULL
                AND document_status = 'FINAL'
                AND organization_id IS NOT NULL
                AND amount_curr - pay_curr > 0
                AND entity_code = :entityCode
                AND (:orgId IS NULL OR organization_id = :orgId::uuid)
                AND acc_type = 'REC'
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
                AND (amount_curr - pay_curr) > 0
                AND organization_id in (:orgId)
                AND document_status = 'FINAL'
                AND acc_type::varchar in (:accType)
                AND (:entityCode is null OR entity_code = :entityCode)
                AND (:startDate is null OR transaction_date >= :startDate::date)
                AND (:endDate is null OR transaction_date <= :endDate::date)
                AND document_value ilike :query
                AND (:accMode is null OR acc_mode::varchar = :accMode)
                AND deleted_at is null
                AND settlement_enabled = true
            ORDER BY transaction_date DESC, id
            LIMIT :limit
            OFFSET :offset
        ), 
         MAPPINGS AS (
        	select jsonb_array_elements(account_utilization_ids)::int as id 
        	from incident_mappings
        	where incident_status = 'REQUESTED'
        	and incident_type = 'SETTLEMENT_APPROVAL'
        )
        SELECT 
            au.id,
            s.source_id,
            s.source_type,
            coalesce(s.amount,0) as settled_tds,
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
            COALESCE(tds_amount, 0) as tds,
            au.currency, 
            au.led_currency, 
            au.sign_flag,
            au.acc_mode,
            CASE WHEN 
            	au.id in (select id from MAPPINGS) 
        	THEN
        		false
        	ELSE
        		true
        	END as approved,
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
        accType: List<AccountType>,
        orgId: List<UUID>,
        entityCode: Int?,
        startDate: Timestamp?,
        endDate: Timestamp?,
        query: String?,
        accMode: AccMode?,
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
                    AND (amount_curr - pay_curr) > 0
                    AND document_status = 'FINAL'
                    AND organization_id in (:orgId)
                    AND acc_type::varchar in (:accType)
                    AND (:entityCode is null OR entity_code = :entityCode)
                    AND (:startDate is null OR transaction_date >= :startDate::date)
                    AND (:endDate is null OR transaction_date <= :endDate::date)
                    AND document_value ilike :query
                    AND deleted_at is null  and is_void = false
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
    suspend fun getDocumentCount(accType: List<AccountType>, orgId: List<UUID>, entityCode: Int?, startDate: Timestamp?, endDate: Timestamp?, query: String?, documentPaymentStatus: String?): Long?

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
    )
}
