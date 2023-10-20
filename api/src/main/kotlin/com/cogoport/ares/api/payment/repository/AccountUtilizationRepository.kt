package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.models.ARLedgerJobDetailsResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.ArOutstandingData
import com.cogoport.ares.api.payment.entity.CustomerOrgOutstanding
import com.cogoport.ares.api.payment.entity.OrgOutstanding
import com.cogoport.ares.api.payment.entity.OrgStatsResponse
import com.cogoport.ares.api.payment.entity.OrgSummary
import com.cogoport.ares.api.payment.entity.OutstandingAgeing
import com.cogoport.ares.api.payment.entity.PaymentData
import com.cogoport.ares.api.payment.entity.SupplierOutstandingAgeing
import com.cogoport.ares.api.payment.model.PaymentUtilizationResponse
import com.cogoport.ares.api.settlement.entity.Document
import com.cogoport.ares.api.settlement.entity.HistoryDocument
import com.cogoport.ares.api.settlement.entity.InvoiceDocument
import com.cogoport.ares.model.balances.GetOpeningBalances
import com.cogoport.ares.model.common.InvoiceBalanceResponse
import com.cogoport.ares.model.common.MonthlyUtilizationCount
import com.cogoport.ares.model.common.PaymentHistoryDetails
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.response.AccPayablesOfOrgRes
import com.cogoport.ares.model.payment.response.AccountPayablesStats
import com.cogoport.ares.model.payment.response.CreditDebitBalance
import com.cogoport.ares.model.payment.response.InvoiceListResponse
import com.cogoport.ares.model.payment.response.OnAccountTotalAmountResponse
import com.cogoport.ares.model.payment.response.OpenInvoiceDetails
import com.cogoport.ares.model.payment.response.OverallStatsForTradeParty
import com.cogoport.ares.model.payment.response.StatsForCustomerResponse
import com.cogoport.ares.model.payment.response.StatsForKamResponse
import com.cogoport.ares.model.settlement.SettlementType
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Date
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepository : CoroutineCrudRepository<AccountUtilization, Long> {
    @NewSpan
    @Query("select exists(select id from account_utilizations where document_no=:documentNo and acc_type=:accType::account_type and deleted_at is null)")
    suspend fun isDocumentNumberExists(documentNo: Long, accType: String): Boolean

    @NewSpan
    @Query("SELECT * FROM account_utilizations WHERE document_no = :documentNo AND acc_type = :accType::account_type LIMIT 1")
    suspend fun findByDocumentNo(documentNo: Long, accType: AccountType): AccountUtilization

    @NewSpan
    @Query(
        """ 
            select 
                *
            from 
                account_utilizations 
            where 
                document_no = :documentNo
            and document_status != 'DELETED'::document_status
            and (:accType is null or acc_type = :accType::account_type) 
            and (:accMode is null or acc_mode = :accMode::account_mode) 
            and deleted_at is null 
            and is_void = false
        """
    )
    suspend fun findRecord(documentNo: Long, accType: String? = null, accMode: String? = null): AccountUtilization?

    @NewSpan
    @Query(
        """
                select
                    *
                from
                    account_utilizations
                where
                    document_value = :documentValue
                and (:accType is null or acc_type= :accType::account_type)
                and (:accMode is null or acc_mode=:accMode::account_mode)
                and deleted_at is null and document_status != 'DELETED'::document_status
            """
    )
    suspend fun findRecordByDocumentValue(documentValue: String, accType: String? = null, accMode: String? = null): AccountUtilization?

    @NewSpan
    @Query("delete from account_utilizations where id=:id")
    suspend fun deleteInvoiceUtils(id: Long): Int

    @NewSpan
    @Query(
        """update account_utilizations set 
              pay_curr = pay_curr + :currencyPay , pay_loc =pay_loc + :ledgerPay , updated_at =now() where id=:id and deleted_at is null"""
    )
    suspend fun updateInvoicePayment(id: Long, currencyPay: BigDecimal, ledgerPay: BigDecimal): Int

    @NewSpan
    @Query(
        """
        select organization_id,
        sum(case when due_date >= now()::date then sign_flag * (amount_loc - pay_loc) else 0 end) as not_due_amount,
        sum(case when (now()::date - due_date) between 1 and 30 then sign_flag * (amount_loc - pay_loc) else 0 end) as thirty_amount,
        sum(case when (now()::date - due_date) between 31 and 60 then sign_flag * (amount_loc - pay_loc) else 0 end) as sixty_amount,
        sum(case when (now()::date - due_date) between 61 and 90 then sign_flag * (amount_loc - pay_loc) else 0 end) as ninety_amount,
        sum(case when (now()::date - due_date) between 91 and 180 then sign_flag * (amount_loc - pay_loc) else 0 end) as oneeighty_amount,
        sum(case when (now()::date - due_date) between 180 and 365 then sign_flag * (amount_loc - pay_loc) else 0 end) as threesixfive_amount,
        sum(case when (now()::date - due_date) > 365 then sign_flag * (amount_loc - pay_loc) else 0 end) as threesixfiveplus_amount,
        sum(case when due_date >= now()::date then 1 else 0 end) as not_due_count,
        sum(case when (now()::date - due_date) between 1 and 30 then 1 else 0 end) as thirty_count,
        sum(case when (now()::date - due_date) between 31 and 60 then 1 else 0 end) as sixty_count,
        sum(case when (now()::date - due_date) between 61 and 90 then 1 else 0 end) as ninety_count,
        sum(case when (now()::date - due_date) between 91 and 180 then 1 else 0 end) as oneeighty_count,
        sum(case when (now()::date - due_date) between 180 and 365 then 1 else 0 end) as threesixfive_count,
        sum(case when (now()::date - due_date) > 365 then 1 else 0 end) as threesixfiveplus_count
        from account_utilizations
        where (:queryName IS NULL OR organization_name ilike :queryName) and (:zone is null or zone_code = :zone) and acc_mode = 'AR'  and is_void = false
        and due_date is not null and document_status in ('FINAL', 'PROFORMA') and organization_id is not null 
        AND ((:orgId) is NULL OR organization_id in (:orgId::uuid)) and  acc_type = 'SINV' and deleted_at is null
        AND (CASE WHEN :flag = 'defaulters' THEN organization_id IN (:defaultersOrgIds)
                 WHEN :flag = 'non_defaulters' THEN (organization_id NOT IN (:defaultersOrgIds) OR (:defaultersOrgIds) is NULL)
            END)
        and  acc_type = 'SINV'
        group by organization_id
        """
    )
    suspend fun getOutstandingAgeingBucket(zone: String?, queryName: String?, orgId: List<UUID>?, page: Int, pageLimit: Int, defaultersOrgIds: List<UUID>?, flag: String): List<OutstandingAgeing>

    @NewSpan
    @Query(
        """
        SELECT
            organization_id,
            max(organization_name) as organization_name,
            sum(
                CASE WHEN (acc_type in('PINV')
                    and(due_date >= now()::date)) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS not_due_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS today_amount,        
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) BETWEEN 1 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS thirty_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS sixty_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS ninety_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS oneeighty_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) BETWEEN 181 AND 365  THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS threesixtyfive_amount,
            sum(
                CASE WHEN acc_type in('PINV')
                    and(now()::date - due_date) > 365 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS threesixtyfiveplus_amount,
            sum(
                CASE WHEN acc_type in('PINV') THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS total_outstanding,
            sum(
                CASE WHEN acc_type in('PCN') THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END) AS total_credit_amount,
            sum(
                CASE WHEN due_date >= now()::date AND acc_type in('PINV') AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS not_due_count,
            sum(
                CASE WHEN (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 AND acc_type in('PINV') AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS today_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 1 AND 30 AND acc_type in('PINV') AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS thirty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 31 AND 60 AND acc_type in('PINV') AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS sixty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 61 AND 90 AND acc_type in('PINV') AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS ninety_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 AND acc_type in('PINV') AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS oneeighty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 181 AND 365 AND acc_type in('PINV') AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS threesixtyfive_count,
            sum(
                CASE WHEN (now()::date - due_date) > 365 AND acc_type in('PINV') AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS threesixtyfiveplus_count,
            sum(
                CASE WHEN (acc_type in('PCN')) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_count
        FROM
            account_utilizations
        WHERE
            (:queryName IS NULL OR organization_name ILIKE :queryName)
            AND (:zone IS NULL OR zone_code = :zone)
            AND (:entityCode IS NULL OR entity_code = :entityCode)
            AND acc_mode = 'AP'
            AND due_date IS NOT NULL
            AND document_status in('FINAL', 'PROFORMA')
            AND organization_id IS NOT NULL
            and(:orgId IS NULL
                OR organization_id = :orgId::uuid)
            AND acc_type in('PINV', 'PCN')
            AND deleted_at IS NULL  and is_void = false
        GROUP BY
            organization_id
        OFFSET GREATEST(0, ((:page - 1) * :pageLimit))
        LIMIT :pageLimit        
        """
    )
    suspend fun getBillsOutstandingAgeingBucket(zone: String?, queryName: String?, orgId: String?, entityCode: Int?, page: Int, pageLimit: Int): List<SupplierOutstandingAgeing>

    @NewSpan
    @Query(
        """
            SELECT
                count(t.organization_id)
            FROM (
                SELECT
                    organization_id
                FROM
                    account_utilizations
                WHERE
                    (:queryName IS NULL OR organization_name ILIKE :queryName)
                    AND (:zone IS NULL OR zone_code = :zone)
                    AND acc_mode = 'AP'
                    AND due_date IS NOT NULL
                    AND document_status in('FINAL', 'PROFORMA')
                    AND organization_id IS NOT NULL
                    AND acc_type = 'PINV'
                    AND deleted_at IS NULL  and is_void = false
                GROUP BY
                    organization_id) AS t 
        """
    )
    suspend fun getBillsOutstandingAgeingBucketCount(zone: String?, queryName: String?, orgId: String?): Int

    @NewSpan
    @Query(
        """
        select organization_id::varchar, currency,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and amount_curr - pay_curr <> 0 then 1 else 0 end) as open_invoices_count,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_curr - pay_curr) else 0 end) as open_invoices_amount,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_loc - pay_loc) else 0 end) as open_invoices_led_amount,
        sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' and amount_curr - pay_curr <> 0 then 1 else 0 end) as payments_count,
        sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then  amount_curr - pay_curr else 0 end) as payments_amount,
        sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then  amount_loc - pay_loc else 0 end) as payments_led_amount,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_curr - pay_curr) else 0 end) + sum(case when acc_type = 'REC' and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end) as outstanding_amount,
        sum(case when acc_type not in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'REC' and document_status = 'FINAL' then sign_flag*(amount_loc - pay_loc) else 0 end) as outstanding_led_amount
        from account_utilizations
        where acc_type in ('SINV','SCN','SDN','REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV', 'SREIMB', 'SREIMBCN') and acc_mode = 'AR' and document_status in ('FINAL', 'PROFORMA') 
        and organization_id = :orgId::uuid and (:zone is null OR zone_code = :zone) and (:entityCode is null OR entity_code = :entityCode) and deleted_at is null
        group by organization_id, currency
        """
    )
    suspend fun generateOrgOutstanding(orgId: String, zone: String?, entityCode: Int?): List<OrgOutstanding>
    @NewSpan
    @Query(
        """
        select organization_id::varchar, currency,
        sum(case when acc_type not in ('PAY', 'PCN', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and amount_curr - pay_curr <> 0 then 1 else 0 end) as open_invoices_count,
        sum(case when acc_type not in ('PAY', 'PCN', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_curr - pay_curr) else 0 end) as open_invoices_amount,
        sum(case when acc_type not in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_loc - pay_loc) else 0 end) as open_invoices_led_amount,
        sum(case when acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' and amount_curr - pay_curr <> 0 then 1 else 0 end) as payments_count,
        sum(case when acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then  amount_curr - pay_curr else 0 end) as payments_amount,
        sum(case when acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then  amount_loc - pay_loc else 0 end) as payments_led_amount,
        sum(case when acc_type not in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_curr - pay_curr) else 0 end) + sum(case when acc_type = 'PAY' and document_status = 'FINAL' then sign_flag*(amount_curr - pay_curr) else 0 end) as outstanding_amount,
        sum(case when acc_type not in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type = 'PAY' and document_status = 'FINAL' then sign_flag*(amount_loc - pay_loc) else 0 end) as outstanding_led_amount
        from account_utilizations
        where acc_type in ('PINV', 'PCN', 'PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV', 'PREIMB') and acc_mode = 'AP' and document_status in ('FINAL', 'PROFORMA') 
        and organization_id = :orgId::uuid and (:zone is null OR zone_code = :zone) and (:entityCode is null OR entity_code = :entityCode) and deleted_at is null
        group by organization_id, currency
        """
    )
    suspend fun generateBillOrgOutstanding(orgId: String, zone: String?, entityCode: Int?): List<OrgOutstanding>

    @NewSpan
    @Query(
        """
        Select
            au.id,
            document_no,
            document_value,
            acc_type,
            amount_curr as amount,
            au.currency as currency,
            amount_curr-pay_curr as current_balance,
            au.led_currency,
            amount_loc as led_amount,
            taxable_amount,
            transaction_date,
            au.sign_flag,
            au.acc_mode,
            coalesce((case when amount_curr != 0 then amount_loc / amount_curr END),1) as exchange_rate,
            '' as status,
            pay_curr as settled_amount,
            au.updated_at as last_edited_date,
            COALESCE(sum(case when s.source_id = au.document_no and s.source_type in ('CTDS','VTDS') then s.amount end), 0) as tds,
            COALESCE(sum(case when s.source_type in ('CTDS','VTDS') then s.amount end), 0) as settled_tds,
            COALESCE((ARRAY_AGG(s1.supporting_doc_url))[1], (ARRAY_AGG(s1.supporting_doc_url))[1]) as supporting_doc_url,
            COALESCE((ARRAY_AGG( CASE WHEN s1.settlement_status IN ('CREATED','POSTING_FAILED') THEN s1.id ELSE NULL END)), null) as not_posted_settlement_ids
            FROM account_utilizations au
            LEFT JOIN settlements s ON
				s.destination_id = au.document_no
				AND s.destination_type::varchar = au.acc_type::varchar
            JOIN settlements s1 ON
				s1.source_id = au.document_no
				AND s1.source_type::varchar = au.acc_type::varchar
            WHERE amount_curr <> 0
                AND pay_curr <> 0
                AND organization_id in (:orgIds)
                AND acc_type::varchar in (:accountTypes)
                AND (:startDate is null or transaction_date >= :startDate::date)
                AND (:endDate is null or transaction_date <= :endDate::date)
                AND (
                    (document_value ilike :query || '%')
                    OR au.document_no in (:paymentIds))
                AND (:entityCode is null OR :entityCode = au.entity_code) 
                AND s.deleted_at is null
                AND au.deleted_at is null
                And au.document_status != 'DELETED'::document_status
            GROUP BY au.id  
            ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'lastEditedDate' THEN au.updated_at
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'lastEditedDate' THEN au.updated_at
                    END        
            END 
            Asc
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    fun getHistoryDocument(
        orgIds: List<UUID>,
        accountTypes: List<String>,
        pageIndex: Int?,
        pageSize: Int?,
        startDate: String?,
        endDate: String?,
        query: String?,
        paymentIds: List<Long>,
        sortBy: String?,
        sortType: String?,
        entityCode: Int?
    ): List<HistoryDocument?>

    @NewSpan
    @Query(
        """
        SELECT count(distinct account_utilizations.id)
            FROM account_utilizations
            JOIN settlements s ON
				s.source_id = document_no
				AND s.source_type::varchar = acc_type::varchar
            WHERE
            amount_curr <> 0
            AND pay_curr <> 0
            AND organization_id in (:orgIds)
            AND acc_type::varchar in (:accountTypes)
            AND (:startDate is null or transaction_date >= :startDate::date)
            AND (:endDate is null or transaction_date <= :endDate::date)
            AND (
                    (document_value ilike :query || '%')
                    OR document_no in (:paymentIds)
                )
            AND s.deleted_at is null
            AND (:entityCode is null OR :entityCode = entity_code)
            AND account_utilizations.deleted_at is null
            
        """
    )
    fun countHistoryDocument(
        orgIds: List<UUID>,
        accountTypes: List<String>,
        startDate: String?,
        endDate: String?,
        query: String?,
        paymentIds: List<Long>,
        entityCode: Int?
    ): Long

    @NewSpan
    @Query(
        """
        SELECT 
            au.id, 
            document_no, 
            document_value, 
            acc_type as account_type,
            organization_id,
            transaction_date as document_date,
            due_date, 
            document_status as invoice_status,
            COALESCE(amount_curr,0) as document_amount, 
            COALESCE(taxable_amount,0) as taxable_amount, 
            COALESCE(pay_curr,0) as settled_amount, 
            COALESCE(amount_curr - pay_curr,0) as balance_amount,
            au.currency, 
            COALESCE(sum(s.amount),0) as settled_tds
                FROM account_utilizations au
                LEFT JOIN settlements s ON 
                    s.destination_id = au.document_no 
                    AND s.destination_type::varchar = au.acc_type::varchar
                    AND s.source_type = 'CTDS'
                WHERE amount_curr <> 0 
                    AND organization_id in (:orgId)
                    AND document_status in ('FINAL', 'PROFORMA')
                    AND (:accType is null OR acc_type::varchar = :accType)
                    AND (:entityCode is null OR entity_code = :entityCode)
                    AND (:startDate is null OR transaction_date >= :startDate::date)
                    AND (:endDate is null OR transaction_date <= :endDate::date)
                    AND (
                        :status is null OR
                        CASE WHEN :status = 'PAID' then  amount_curr = pay_curr
                        WHEN :status = 'UNPAID' then  pay_curr = 0
                        WHEN :status = 'PARTIAL_PAID' then  (amount_curr - pay_curr) <> 0 AND (pay_curr > 0)
                        END
                        )
                    AND (:query is null OR document_value ilike :query)
                    AND s.deleted_at is null
                    AND au.deleted_at is null
                GROUP BY au.id
                LIMIT :limit
                OFFSET :offset
        """
    )
    suspend fun getInvoiceDocumentList(limit: Int? = null, offset: Int? = null, accType: AccountType?, orgId: List<UUID>, entityCode: Int?, startDate: Timestamp?, endDate: Timestamp?, query: String?, status: String?): List<InvoiceDocument?>

    @NewSpan
    @Query(
        """
        WITH FILTERS AS (
            SELECT id 
            FROM account_utilizations
            WHERE amount_curr <> 0 
                AND pay_curr <> 0
                AND organization_id in (:orgId)
                AND document_status = 'FINAL'
                AND (:accMode is null OR acc_mode = :accMode::ACCOUNT_MODE)
                AND (:accType is null OR acc_type::varchar = :accType)
                AND (:startDate is null OR transaction_date >= :startDate::date)
                AND (:endDate is null OR transaction_date <= :endDate::date)
                AND document_value ilike :query
                AND deleted_at is null
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
            COALESCE(taxable_amount, 0) as taxable_amount, 
            COALESCE(amount_curr, 0) as after_tds_amount, 
            COALESCE(pay_curr, 0) as settled_amount, 
            COALESCE(amount_curr - pay_curr, 0) as balance_amount,
            COALESCE(amount_loc - pay_loc, 0) as document_led_balance,
            COALESCE(tds_amount, 0) as tds,
            au.currency, 
            au.led_currency, 
            au.sign_flag,
            au.acc_mode,
            au.migrated,
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
                END
                , 1) AS exchange_rate
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
             AND au.deleted_at is null  and au.is_void = false
            AND p.deleted_at is null
            AND s.deleted_at is null
            ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'dueDate' THEN au.due_date
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'transactionDate' THEN au.transaction_date
                         WHEN :sortBy = 'dueDate' THEN au.due_date
                    END        
            END 
            Asc
        """
    )
    suspend fun getTDSDocumentList(
        limit: Int? = null,
        offset: Int? = null,
        accType: AccountType?,
        orgId: List<UUID>,
        accMode: AccMode?,
        startDate: Timestamp?,
        endDate: Timestamp?,
        query: String?,
        sortBy: String?,
        sortType: String?
    ): List<Document?>

    @NewSpan
    @Query(
        """
        SELECT 
            count(id)
                FROM account_utilizations
                WHERE 
                    amount_curr <> 0
                    AND document_status in ('FINAL','PROFORMA')
                    AND organization_id in (:orgId)
                    AND (:accType is null OR acc_type::varchar = :accType)
                    AND (:entityCode is null OR entity_code = :entityCode)
                    AND (:startDate is null OR transaction_date >= :startDate::date)
                    AND (:endDate is null OR transaction_date <= :endDate::date)
                    AND (
                        :status is null OR
                        CASE WHEN :status = 'PAID' then  amount_curr = pay_curr
                        WHEN :status = 'UNPAID' then  pay_curr = 0
                        WHEN :status = 'PARTIAL_PAID' then  (amount_curr - pay_curr) <> 0 AND (pay_curr > 0)
                        END
                        )
                    AND (:query is null OR document_value ilike :query)
                    AND deleted_at is null  and is_void = false
    """
    )
    suspend fun getInvoiceDocumentCount(accType: AccountType?, orgId: List<UUID>, entityCode: Int?, startDate: Timestamp?, endDate: Timestamp?, query: String?, status: String?): Long?

    @NewSpan
    @Query(
        """
        SELECT 
            count(id)
                FROM account_utilizations
                WHERE amount_curr <> 0
                    AND pay_curr <> 0
                    AND document_status = 'FINAL'
                    AND organization_id in (:orgId)
                    AND (:accType is null OR acc_type::varchar = :accType)
                    AND ((:accMode) is null OR acc_mode::varchar = :accMode)
                    AND (:startDate is null OR transaction_date >= :startDate::date)
                    AND (:endDate is null OR transaction_date <= :endDate::date)
                    AND document_value ilike :query
                    AND deleted_at is null and is_void = false
    """
    )
    suspend fun getTDSDocumentCount(accType: AccountType?, orgId: List<UUID>, accMode: AccMode?, startDate: Timestamp?, endDate: Timestamp?, query: String?): Long?

    @NewSpan
    @Query(
        """
            SELECT coalesce(sum(sign_flag*(amount_loc-pay_loc)),0) as amount
                FROM account_utilizations
                WHERE document_status = 'FINAL'
                    AND entity_code = :entityCode
                    AND organization_id in (:orgId)
                    AND (:startDate is null or transaction_date >= :startDate)
                    AND (:endDate is null or transaction_date <= :endDate)
                    AND ((:accType) is null or acc_type::varchar in (:accType))
                    AND ((:accMode) is null or acc_mode::varchar in (:accMode))
                    AND deleted_at is null  and is_void = false
                    and acc_type != 'NEWPR'
        """
    )
    suspend fun getAccountBalance(orgId: List<UUID>, entityCode: Int, startDate: Timestamp?, endDate: Timestamp?, accType: List<AccountType>?, accMode: List<String>?): BigDecimal

    @NewSpan
    @Query(
        """
            SELECT 
                organization_id as org_id,
                organization_name as org_name,
                led_currency as currency,
                sum(
                    CASE WHEN 
                        acc_mode::varchar = :accMode AND
                        acc_type NOT IN ('REC', 'PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') 
                    THEN 
                        sign_flag * (amount_loc - pay_loc) else 0 END
                ) + 
                sum(
                    CASE WHEN 
                        acc_mode::varchar = :accMode AND
                        acc_type IN ('REC','PAY') AND document_status = 'FINAL' 
                    THEN 
                        sign_flag * (amount_loc - pay_loc) else 0 END
                ) as outstanding
                FROM account_utilizations
                WHERE organization_id = :orgId
                    AND document_status in ('FINAL', 'PROFORMA')
                    AND (:startDate is null or transaction_date >= :startDate)
                    AND (:endDate is null or transaction_date <= :endDate)
                    AND deleted_at is null
                    GROUP BY organization_id, organization_name, led_currency
        """
    )
    suspend fun getOrgSummary(orgId: UUID, accMode: AccMode, startDate: Timestamp?, endDate: Timestamp?): OrgSummary?

    @NewSpan
    @Query(
        """
        SELECT 
            :orgId as organization_id,
            MAX(ledger_currency) as ledger_currency,
            SUM(COALESCE(open_receivables,0) + COALESCE(on_account_receivables,0)) as receivables,
            SUM(COALESCE(open_payables,0) + COALESCE(on_account_payables,0)) as payables
        FROM (
            SELECT
                MAX(led_currency) as ledger_currency,
                CASE WHEN acc_type in ('SINV','SCN') then SUM(sign_flag*(amount_loc - pay_loc)) end as open_receivables,
                CASE WHEN acc_type in ('PINV','PDN','PCN') then SUM(sign_flag*(amount_loc - pay_loc)) end as open_payables,
                CASE WHEN acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then SUM(sign_flag*(amount_loc-pay_loc)) end as on_account_receivables,
                CASE WHEN acc_type in ('PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') then SUM(sign_flag*(amount_loc-pay_loc)) end as on_account_payables
            FROM account_utilizations
            WHERE 
                acc_type in ('PDN','SCN','REC','PINV','PCN','SINV','PAY', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV')
                AND organization_id = :orgId
                AND deleted_at is null
            GROUP BY  acc_type
        ) A
    """
    )
    suspend fun getOrgStats(orgId: UUID): OrgStatsResponse?

    @NewSpan
    @Query(
        """
        SELECT 
            :orgId as organization_id,
            MAX(ledger_currency) as ledger_currency,
            SUM(COALESCE(open_payables,0)) as payables,
            null as receivables
        FROM (
            SELECT
                MAX(led_currency) as ledger_currency,
                CASE WHEN acc_type in ('PINV','PDN') then SUM(sign_flag*(amount_loc - pay_loc)) end as open_payables
            FROM account_utilizations
            WHERE 
                acc_type in ('PDN', 'PINV')
                AND organization_id = :orgId
                AND deleted_at is null
            GROUP BY  acc_type
        ) A
    """
    )
    suspend fun getOrgStatsForCoeFinance(orgId: UUID): OrgStatsResponse?

    @NewSpan
    @Query(
        """             
            SELECT organization_id, acc_type, acc_mode, SUM(amount_curr - pay_curr) as payment_value 
            FROM account_utilizations 
            WHERE acc_type::varchar = :accType and acc_mode::varchar =:accMode and document_status != 'DELETED' and organization_id in (:organizationIdList) 
            GROUP BY organization_id, acc_type, acc_mode
        """
    )
    suspend fun onAccountPaymentAmount(accType: AccountType, accMode: AccMode, organizationIdList: List<UUID>): MutableList<OnAccountTotalAmountResponse>

    @NewSpan
    @Query(
        """
        SELECT 
            document_no,
            transaction_date::timestamp AS transaction_date, 
            coalesce((case when amount_curr != 0 then amount_loc / amount_curr END),1) as exchange_rate
        FROM account_utilizations
        WHERE acc_type::varchar in (:documentType) 
        AND document_no in (:documentNo)
        AND deleted_at is null
    """
    )
    suspend fun getPaymentDetails(documentNo: List<Long>, documentType: List<SettlementType>): List<PaymentData>

    @NewSpan
    @Query(
        """
            SELECT
                *
            FROM
                account_utilizations
            WHERE
                document_value = :documentValue
            AND acc_type = :accType::account_type
            AND deleted_at is null and is_void = false
            AND document_status != 'DELETED'::document_status
        """
    )
    suspend fun getAccountUtilizationsByDocValue(documentValue: String, accType: AccountType?): AccountUtilization

    @NewSpan
    @Query(
        """
                SELECT
                    *
                FROM
                    account_utilizations
                WHERE 
                    document_no = :documentNo
                AND acc_type = :accType::account_type
                AND deleted_at is null and is_void = false
                AND document_status != 'DELETED'::document_status
            """
    )
    suspend fun getAccountUtilizationsByDocNo(documentNo: String, accType: AccountType): AccountUtilization

    @NewSpan
    @Query(
        """ 
        SELECT
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_proforma_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN 1 ELSE 0 END),0) as proforma_invoices_count,
        (SELECT count(distinct tagged_organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') AND amount_loc - pay_loc <> 0 AND document_value in (:documentValues) AND document_status = 'PROFORMA' AND deleted_at is null) as customers_count_proforma,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date > now()::date AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_due_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND (amount_loc- pay_loc <> 0) AND due_date > now()::date AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as due_invoices_count,
        (SELECT count(distinct tagged_organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') AND amount_loc - pay_loc <> 0 AND document_value in (:documentValues) AND document_status in ('FINAL','PROFORMA') AND due_date > now()::date AND deleted_at is null) as customers_count_due,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date AND (amount_loc- pay_loc <> 0) AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as overdue_invoices_count,
        (SELECT count(distinct tagged_organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') AND amount_curr - pay_curr <> 0 AND document_value in (:documentValues) AND document_status in ('FINAL','PROFORMA') AND due_date <= now()::date AND deleted_at is null) as customers_count_overdue,
        COALESCE(sum(case when acc_type in ('SINV','SCN','SDN') AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_amount_receivables,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status in ('FINAL','PROFORMA') AND (amount_loc- pay_loc <> 0) AND document_status in ('FINAL','PROFORMA') THEN 1 ELSE 0 END),0) as receivables_invoices_count,
        (SELECT count(distinct tagged_organization_id) from account_utilizations where acc_type in ('SINV','SDN','SCN') AND amount_loc - pay_loc <> 0 AND document_value in (:documentValues) AND document_status in ('FINAL','PROFORMA') AND deleted_at is null) as customers_count_receivables,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 AND due_date is NOT NULL THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60 AND due_date is NOT NULL THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 AND due_date is NOT NULL THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(sum(case when (now()::date - due_date) >90 AND due_date is NOT NULL THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) AND due_date is NOT NULL THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) AND due_date is NOT NULL THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) AND due_date is NOT NULL THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(sum(case when (now()::date - due_date) >90 AND (amount_loc - pay_loc <> 0) AND due_date is NOT NULL THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value in (:documentValues) AND tagged_organization_id IS NOT NULL AND deleted_at is null  and is_void = false
        """
    )
    suspend fun getOverallStats(documentValues: List<String>): StatsForKamResponse

    @NewSpan
    @Query(
        """ 
        SELECT * from
        (SELECT tagged_organization_id as organization_id,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_proforma_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN 1 ELSE 0 END),0) as proforma_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date > now()::date AND  document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_due_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND (amount_loc - pay_loc <> 0) AND due_date > now()::date AND document_status = 'FINAL' THEN 1 ELSE 0 END),0) as due_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date AND (amount_loc- pay_loc <> 0) AND document_status = 'FINAL' THEN 1 ELSE 0 END),0) as overdue_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SCN','SDN') AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_amount_receivables,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'FINAL' AND (amount_loc- pay_loc <> 0) AND document_status = 'FINAL' THEN 1 ELSE 0 END),0) as receivables_invoices_count,
        COALESCE(abs(sum(case when acc_type = 'REC' AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END)),0) as on_account_payment,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60  THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(sum(case when (now()::date - due_date) > 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(sum(case when (now()::date - due_date) > 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND ((:documentValues) is NULL OR document_value in (:documentValues)) AND due_date IS NOT NULL AND  amount_curr <> 0 AND tagged_organization_id IS NOT NULL 
        AND (:bookingPartyId is NULL OR tagged_organization_id = :bookingPartyId::uuid) AND deleted_at is null  and is_void = false
        GROUP BY tagged_organization_id) output
        ORDER BY
            CASE WHEN :sortBy = 'Desc' THEN
                    CASE WHEN :sortType = 'proforma_invoices_count' THEN output.proforma_invoices_count
                         WHEN :sortType = 'overdue_invoices_count' THEN output.overdue_invoices_count
                         WHEN :sortType = 'due_invoices_count' THEN output.due_invoices_count
                    END
            END 
            Desc,
            CASE WHEN :sortBy = 'Asc' THEN
                    CASE WHEN :sortType = 'proforma_invoices_count' THEN output.proforma_invoices_count
                         WHEN :sortType = 'overdue_invoices_count' THEN output.overdue_invoices_count
                         WHEN :sortType = 'due_invoices_count' THEN output.due_invoices_count 
                    END        
            END 
            Asc,
            CASE WHEN :sortType is NULL and :sortBy is NULL THEN output.due_invoices_count END Desc
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    suspend fun getOverallStatsForCustomers(
        documentValues: List<String>?,
        bookingPartyId: String?,
        pageIndex: Int,
        pageSize: Int,
        sortType: String?,
        sortBy: String?
    ): List<StatsForCustomerResponse?>

    @NewSpan
    @Query(
        """ 
        SELECT count(*) from
        (SELECT tagged_organization_id as organization_id,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_proforma_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN 1 ELSE 0 END),0) as proforma_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date > now()::date AND  document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_due_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND (amount_loc - pay_loc <> 0) AND due_date > now()::date AND document_status = 'FINAL' THEN 1 ELSE 0 END),0) as due_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date AND (amount_loc- pay_loc <> 0) AND document_status = 'FINAL' THEN 1 ELSE 0 END),0) as overdue_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SCN','SDN') AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_amount_receivables,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'FINAL' AND (amount_loc- pay_loc <> 0) AND document_status = 'FINAL' THEN 1 ELSE 0 END),0) as receivables_invoices_count,
        COALESCE(abs(sum(case when acc_type = 'REC' AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END)),0) as on_account_payment,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60  THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(sum(case when (now()::date - due_date) > 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(sum(case when (now()::date - due_date) > 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND ((:documentValues) is NULL OR document_value in (:documentValues)) AND due_date IS NOT NULL AND  amount_curr <> 0 AND tagged_organization_id IS NOT NULL 
        AND (:bookingPartyId is NULL OR tagged_organization_id = :bookingPartyId::uuid) AND deleted_at is null  and is_void = false
        GROUP BY tagged_organization_id) as output
        """
    )
    suspend fun getCount(
        documentValues: List<String>?,
        bookingPartyId: String?
    ): Long?

    @NewSpan
    @Query(
        """
            DELETE FROM account_utilizations WHERE document_value IN (:docValues) and acc_type IN ('SINV', 'SDN') and acc_mode='AR' and document_status='PROFORMA' 
        """
    )
    suspend fun deleteConsolidatedInvoices(docValues: List<String>)

    @NewSpan
    @Query("select current_date - min(due_date) from account_utilizations au where amount_loc - pay_loc != 0 and acc_type = 'SINV' and document_no in (:invoiceIds) AND deleted_at is null")
    suspend fun getCurrentOutstandingDays(invoiceIds: List<Long>): Long

    @NewSpan
    @Query(
        """
            UPDATE account_utilizations SET deleted_at = NOW() WHERE id = :id
        """
    )
    suspend fun deleteAccountUtilization(id: Long)

    @NewSpan
    @Query(
        """UPDATE account_utilizations SET 
              pay_curr = :currencyPay , pay_loc = :ledgerPay , updated_at = NOW() WHERE id =:id AND deleted_at is null  and is_void = false"""
    )
    suspend fun updateAccountUtilization(id: Long, status: DocumentStatus, currencyPay: BigDecimal, ledgerPay: BigDecimal)

    @NewSpan
    @Query(
        """
            SELECT id,pay_curr,pay_loc FROM account_utilizations WHERE document_no = :paymentNum AND acc_mode = 'AP' AND deleted_at is null and is_void = false
        """
    )
    suspend fun getDataByPaymentNum(paymentNum: Long?): PaymentUtilizationResponse

    @NewSpan
    @Query(
        """ 
        SELECT organization_id,
        COALESCE(SUM(CASE WHEN acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(SUM(CASE WHEN acc_type in ('SINV','SDN','SCN') then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_loc - pay_loc) else 0 end),0) as total_outstanding_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 0 AND 30 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 31 AND 60  THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 61 AND 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) > 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) > 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value IN (:documentValues) AND due_date IS NOT NULL AND  amount_curr <> 0 AND organization_id IS NOT NULL 
        AND deleted_at IS NULL and is_void = false
        GROUP BY organization_id
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    suspend fun getOverallStatsForTradeParty(
        documentValues: List<String>,
        pageIndex: Int,
        pageSize: Int
    ): List<OverallStatsForTradeParty?>

    @NewSpan
    @Query(
        """ 
        SELECT COUNT(*) FROM
        (SELECT organization_id,
        COALESCE(SUM(CASE WHEN acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status in ('FINAL','PROFORMA') THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(SUM(CASE WHEN acc_type in ('SINV','SDN','SCN') then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type in ('REC', 'OPDIV', 'MISC', 'BANK', 'CONTR', 'INTER', 'MTC', 'MTCCV') and document_status = 'FINAL' then sign_flag*(amount_loc - pay_loc) else 0 end),0) as total_outstanding_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 0 AND 30 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 31 AND 60  THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 61 AND 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) > 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(SUM(CASE WHEN (now()::date - due_date) > 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value IN (:documentValues) AND due_date IS NOT NULL AND  amount_curr <> 0 AND organization_id IS NOT NULL 
        AND deleted_at IS NULL  and is_void = false
        GROUP BY organization_id) as output
        """
    )
    suspend fun getTradePartyCount(
        documentValues: List<String>
    ): Long?

    @NewSpan
    @Query(
        """ 
         SELECT * FROM
        (SELECT organization_id::VARCHAR,
        document_value as document_number ,
        document_status AS document_type,
        service_type,
        amount_loc AS invoice_amount,
        sign_flag*(amount_loc - pay_loc) as outstanding_amount
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value IN (:documentValues) AND organization_id IS NOT NULL 
        AND deleted_at IS NULL  and is_void = false ) output
        ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                    CASE WHEN :sortBy = 'invoice_amount' THEN output.invoice_amount
                         WHEN :sortBy = 'outstanding_amount' THEN output.outstanding_amount
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                    CASE WHEN :sortBy = 'invoice_amount' THEN output.invoice_amount
                         WHEN :sortBy = 'outstanding_amount' THEN output.outstanding_amount 
                    END        
            END 
            Asc,
            CASE WHEN :sortType is NULL and :sortBy is NULL THEN output.invoice_amount END Desc
        OFFSET GREATEST(0, ((:pageIndex - 1) * :pageSize)) LIMIT :pageSize
        """
    )
    suspend fun getInvoiceListForTradeParty(
        documentValues: List<String>,
        sortBy: String?,
        sortType: String?,
        pageIndex: Int,
        pageSize: Int
    ): List<InvoiceListResponse>

    @NewSpan
    @Query(
        """ 
         SELECT COUNT(*) FROM
        (SELECT organization_id::VARCHAR,
        document_value as document_number ,
        document_status AS document_type,
        service_type,
        amount_loc AS invoice_amount,
        sign_flag*(amount_loc - pay_loc) as outstanding_amount
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND document_value IN (:documentValues) AND organization_id IS NOT NULL 
        AND deleted_at IS NULL and is_void = false) as output
        """
    )
    suspend fun getInvoicesCountForTradeParty(
        documentValues: List<String>
    ): Long?

    @NewSpan
    @Query(
        """ 
        SELECT DISTINCT organization_id
        FROM account_utilizations
        WHERE acc_mode = :accMode::account_mode AND organization_id IS NOT NULL 
        AND deleted_at IS NULL and is_void = false
        """
    )
    suspend fun getTradePartyOrgIds(accMode: AccMode?): List<UUID>

    @NewSpan
    @Query(
        """
            select organization_id::varchar, currency,
            sum(case when acc_type in ('SINV', 'SREIMB') and amount_curr - pay_curr <> 0 and document_status = 'FINAL' then 1 else 0 end) as open_invoices_count,
            sum(case when acc_type in ('SINV', 'SREIMB') and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end) as open_invoices_amount,
            sum(case when acc_type in ('SINV', 'SREIMB') and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) as open_invoices_led_amount,
            sum(case when acc_type in ('SCN', 'SREIMBCN') and amount_curr - pay_curr <> 0 and document_status = 'FINAL' then 1 else 0 end) as credit_note_count,
            sum(case when acc_type in ('SCN', 'SREIMBCN') and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end) as credit_note_amount,
            sum(case when acc_type in ('SCN', 'SREIMBCN') and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) as credit_note_led_amount,
            sum(case when acc_type in ('REC', 'CTDS') and abs(amount_curr - pay_curr) > 0.001 and document_status = 'FINAL' then 1 when acc_type IN ('BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') and amount_curr - pay_curr <> 0 and document_status = 'FINAL' then 1 else 0 end) as payments_count,
            sum(case when acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end) as payments_amount,
            sum(case when acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) as payments_led_amount,
            sum(case when acc_type in ('SINV', 'SREIMB') and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end) + sum(case when acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end) + sum(case when acc_type in ('SCN', 'SREIMBCN') and document_status = 'FINAL' then sign_flag * (amount_curr - pay_curr) else 0 end) as outstanding_amount,
            sum(case when acc_type in ('SINV', 'SREIMB') and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type in ('REC', 'CTDS','BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) + sum(case when acc_type in ('SCN', 'SREIMBCN') and document_status = 'FINAL' then sign_flag * (amount_loc - pay_loc) else 0 end) as outstanding_led_amount
            from account_utilizations
            where acc_type in ('SINV','SCN','REC', 'CTDS', 'SREIMB', 'SREIMBCN', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC') and acc_mode = 'AR' and document_status = 'FINAL'  
            and organization_id = :orgId::uuid and entity_code = :entityCode and deleted_at is null
            group by organization_id, currency
        """
    )
    suspend fun generateCustomerOutstanding(orgId: String, entityCode: Int): List<CustomerOrgOutstanding>

    @NewSpan
    @Query(
        """
        SELECT SUM(sign_flag*(amount_loc-pay_loc)) FROM account_utilizations 
        WHERE acc_mode = 'AP' AND acc_type IN ('PCN','PREIMB','PINV') AND deleted_at IS NULL AND migrated = false AND 
        CASE WHEN :entity IS NOT NULL THEN entity_code = :entity ELSE TRUE END
    """
    )
    suspend fun getAccountPayables(entity: Int?): BigDecimal

    @NewSpan
    @Query(
        """
        SELECT entity_code, led_currency, SUM(sign_flag*(amount_loc-pay_loc)) as account_payables FROM account_utilizations 
        WHERE acc_mode = 'AP' AND acc_type IN ('PCN','PREIMB','PINV') AND deleted_at IS NULL AND migrated = false AND 
        tagged_organization_id = :organizationId AND CASE WHEN :entity IS NOT NULL THEN entity_code = :entity ELSE TRUE END GROUP BY entity_code, led_currency
    """
    )
    suspend fun getApPerOrganization(organizationId: String?, entity: Int?): List<AccPayablesOfOrgRes>

    @NewSpan
    @Query(
        """
        SELECT SUM(CASE WHEN acc_type IN ('PINV','PREIMB') THEN (amount_loc-pay_loc) ELSE 0 END) AS open_invoice_amount,
        SUM(CASE WHEN acc_type in ('PINV','PREIMB') THEN 1 ELSE 0 END) AS open_invoice_count,
        SUM(CASE WHEN acc_type IN ('PAY') THEN (amount_loc-pay_loc) ELSE 0 END) AS on_account_amount,
        SUM(CASE WHEN acc_type IN ('PCN') THEN (amount_loc-pay_loc) ELSE 0 END) AS credit_note_amount
        FROM account_utilizations WHERE acc_mode = 'AP' AND deleted_at IS NULL AND migrated = false AND
        CASE WHEN :entity IS NOT NULL THEN entity_code = :entity ELSE TRUE END
    """
    )
    suspend fun getAccountPayablesStats(entity: Int?): AccountPayablesStats
    @NewSpan
    @Query(
        """
        SELECT  COUNT(distinct trade_party_mapping_id) FROM account_utilizations
        WHERE acc_mode = 'AP' AND acc_type IN ('PINV','PREIMB') AND deleted_at IS NULL AND migrated = false AND amount_curr > pay_curr AND
        CASE WHEN :entity IS NOT NULL THEN entity_code = :entity ELSE TRUE END
    """
    )
    suspend fun getOrganizationCount(entity: Int?): Long?

    @Query(
        """
        SELECT
            organization_id as trade_party_detail_id,
            SUM((amount_loc - pay_loc) * sign_flag) AS balance_amount,
            led_currency as ledger_currency
        FROM
            account_utilizations
        WHERE
            document_status = 'FINAL'
            AND entity_code = :entityCode 
            AND transaction_date <= :transactionDate::DATE
            AND acc_type != 'NEWPR'
            AND deleted_at IS NULL
        GROUP BY
            trade_party_detail_id, led_currency
    """
    )
    suspend fun getLedgerBalances(transactionDate: Date, entityCode: Int): List<GetOpeningBalances>?

    @NewSpan
    @Query(
        """
            UPDATE 
                account_utilizations 
            SET 
                deleted_at = NOW(), 
                updated_at = NOW(),
                document_status = 'DELETED'::document_status,
                settlement_enabled = FALSE
            WHERE 
                document_value = :docValue 
            AND 
                acc_type = :accType::ACCOUNT_TYPE
            AND
                deleted_at IS NULL
            AND
                document_status != 'DELETED'::document_status
        """
    )
    suspend fun deleteAccountUtilizationByDocumentValueAndAccType(docValue: String?, accType: AccountType?)

    @NewSpan
    @Query(
        """
            update account_utilizations 
            set 
            deleted_at = NOW(), 
            updated_at = NOW(),
            document_status = 'DELETED'::document_status,
            settlement_enabled = FALSE
            where document_value in (:documentValues)
        """
    )
    suspend fun updateAccountUtilizationUsingDocValue(documentValues: List<String>)

    @NewSpan
    @Query(
        """
            SELECT
                document_value,
                COALESCE(sign_flag * (amount_loc - pay_loc), 0) AS led_balance_amount,
                COALESCE(sign_flag * (amount_curr - pay_curr), 0) AS balance_amount
            FROM
                account_utilizations
            where
                document_value in (:invoiceNumbers)
                AND acc_mode = :accMode::account_mode
        """
    )
    suspend fun getInvoiceBalanceAmount(invoiceNumbers: List<String>, accMode: AccMode): List<InvoiceBalanceResponse>?

    @NewSpan
    @Query(
        """ 
        SELECT tagged_organization_id as organization_id,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_proforma_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'PROFORMA' THEN 1 ELSE 0 END),0) as proforma_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date > now()::date AND  document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_due_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND (amount_loc - pay_loc <> 0) AND due_date > now()::date AND document_status = 'FINAL' THEN 1 ELSE 0 END),0) as due_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date  AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_overdue_amount,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND due_date <= now()::date AND (amount_loc- pay_loc <> 0) AND document_status = 'FINAL' THEN 1 ELSE 0 END),0) as overdue_invoices_count,
        COALESCE(sum(case when acc_type in ('SINV','SCN','SDN') AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END),0) as total_amount_receivables,
        COALESCE(sum(case when acc_type in ('SINV','SDN','SCN') AND document_status = 'FINAL' AND (amount_loc- pay_loc <> 0) AND document_status = 'FINAL' THEN 1 ELSE 0 END),0) as receivables_invoices_count,
        COALESCE(abs(sum(case when acc_type = 'REC' AND document_status = 'FINAL' THEN sign_flag*(amount_loc - pay_loc) ELSE 0 END)),0) as on_account_payment,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_thirty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60  THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_sixty_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_days_amount,
        COALESCE(sum(case when (now()::date - due_date) > 90 THEN sign_flag * (amount_loc - pay_loc) ELSE 0 END),0) as due_by_ninety_plus_days_amount,
        COALESCE(sum(case when (now()::date - due_date) between 0 AND 30 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_thirty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 31 AND 60 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_sixty_days_count,
        COALESCE(sum(case when (now()::date - due_date) between 61 AND 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_days_count,
        COALESCE(sum(case when (now()::date - due_date) > 90 AND (amount_loc - pay_loc <> 0) THEN 1 ELSE 0 END),0) as due_by_ninety_plus_days_count
        FROM account_utilizations
        WHERE acc_mode = 'AR' AND due_date IS NOT NULL AND  amount_curr <> 0 AND tagged_organization_id IS NOT NULL 
        AND (COALESCE(:bookingPartyIds) IS NULL OR tagged_organization_id::VARCHAR IN (:bookingPartyIds)) AND deleted_at is null  and is_void = false
        GROUP BY tagged_organization_id
        """
    )
    suspend fun getOverallStatsForMultipleCustomers(
        bookingPartyIds: List<String>?
    ): List<StatsForCustomerResponse?>

    @NewSpan
    @Query(
        """
            SELECT
                SUM(CASE WHEN transaction_date >= date_trunc('month', current_date - interval '1' month)
                    and transaction_date < date_trunc('month', current_date) THEN 1 ELSE 0 END) as last_month,
                SUM(CASE WHEN transaction_date >= date_trunc('month', current_date - interval '2' month)
                    and transaction_date < date_trunc('month', current_date - interval '1' month) THEN 1 ELSE 0 END) as second_last_month,
                SUM(CASE WHEN transaction_date >= date_trunc('month', current_date - interval '3' month)
                    and transaction_date < date_trunc('month', current_date - interval '2' month) THEN 1 ELSE 0 END) as third_last_month,
                SUM(CASE WHEN transaction_date >= date_trunc('month', current_date - interval '4' month)
                    and transaction_date < date_trunc('month', current_date - interval '3' month) THEN 1 ELSE 0 END) as fourth_last_month,
                SUM(CASE WHEN transaction_date >= date_trunc('month', current_date - interval '5' month)
                    and transaction_date < date_trunc('month', current_date - interval '4' month) THEN 1 ELSE 0 END) as fifth_last_month,
                SUM(CASE WHEN transaction_date >= date_trunc('month', current_date - interval '6' month)
                    and transaction_date < date_trunc('month', current_date - interval '5' month) THEN 1 ELSE 0 END) as sixth_last_month
            FROM account_utilizations
            WHERE  (:accMode IS NULL OR acc_mode::VARCHAR = :accMode)
            AND (COALESCE(:accTypes) IS NULL OR acc_type::VARCHAR IN (:accTypes))
            AND document_status = 'FINAL'
            AND (:organizationId IS NULL OR organization_id = :organizationId)
            AND (:entityCodes IS NULL OR entity_code IN (:entityCodes))
        """
    )
    suspend fun getMonthlyUtilizationCounts(accMode: AccMode?, accTypes: List<AccountType>?, organizationId: UUID?, entityCodes: List<Int>?): MonthlyUtilizationCount

    @NewSpan
    @Query(
        """
            WITH payment_history AS (
                SELECT
                    document_value, 
                    (array_agg(ac.due_date::date - s.settlement_date::date order by s.created_at asc))[1] as days,
                    (array_agg(CASE WHEN (ac.due_date::date - ac.transaction_date::date) = 0 THEN 'cash' ELSE 'credit' END))[1] as payment_mode
                FROM
                account_utilizations AS ac
                INNER JOIN settlements AS s ON s.destination_id = ac.document_no
                WHERE  (:accMode IS NULL OR ac.acc_mode::VARCHAR = :accMode)
                AND (COALESCE(:accTypes) IS NULL OR ac.acc_type::VARCHAR IN (:accTypes))
                AND (:organizationId IS NULL OR ac.organization_id = :organizationId)
                AND (:entityCodes IS NULL OR ac.entity_code IN (:entityCodes))
                AND (COALESCE(:destinationTypes) IS NULL OR s.destination_type::VARCHAR in (:destinationTypes))
                AND s.deleted_at is null
                AND ac.deleted_at is null
                GROUP BY
                ac.document_value
            )
            SELECT
            SUM(
                CASE WHEN ((payment_mode = 'credit') and (days < 0)) OR ((payment_mode = 'cash') and ((days + 3) < 0)) THEN 1 ELSE 0 END
            ) AS delayed_payments,
            COALESCE(COUNT(*), 0) AS total_payments
            FROM payment_history
        """
    )
    suspend fun getPaymentHistoryDetails(
        accMode: AccMode?,
        accTypes: List<AccountType>?,
        organizationId: UUID?,
        entityCodes: List<Int>?,
        destinationTypes: List<SettlementType>?
    ): PaymentHistoryDetails

    @NewSpan
    @Query(
        """
          SELECT au.transaction_date::varchar AS transaction_date,
            au.acc_type as document_type,
            au.document_value::varchar AS document_number,
            au.currency as currency,
            au.amount_curr::varchar AS amount,
            CASE WHEN au.sign_flag = -1 THEN au.amount_loc ELSE 0 END AS credit,
            CASE WHEN au.sign_flag = 1 THEN au.amount_loc ELSE 0 END AS debit,
            p.trans_ref_number AS transaction_ref_number,
            '' AS shipment_document_number,
            '' AS house_document_number
            FROM account_utilizations au
            LEFT JOIN payments p ON p.payment_num = au.document_no AND p.payment_num_value = au.document_value
            WHERE au.acc_mode::VARCHAR = :accMode AND au.organization_id = :organizationId::UUID AND document_status = 'FINAL'
            AND au.transaction_date >= :startDate::DATE AND au.transaction_date <= :endDate::DATE AND au.entity_code IN (:entityCodes)
            AND au.deleted_at IS NULL AND au.acc_type NOT IN ('NEWPR', 'MTCCV') AND p.deleted_at IS NULL
            ORDER BY transaction_date
        """
    )
    suspend fun getARLedger(accMode: AccMode, organizationId: String, entityCodes: List<Int>, startDate: LocalDate, endDate: LocalDate): List<ARLedgerJobDetailsResponse>

    @NewSpan
    @Query(
        """
            SELECT
            (array_agg(led_currency))[1] AS ledger_currency,
            COALESCE(SUM(CASE WHEN au.sign_flag = -1 THEN (au.amount_loc) ELSE 0 END), 0) AS credit,
            COALESCE(SUM(CASE WHEN au.sign_flag = 1 THEN (au.amount_loc) ELSE 0 END), 0) AS debit
            FROM account_utilizations au 
            WHERE au.acc_mode::VARCHAR = :accMode AND au.organization_id = :organizationId::UUID AND document_status = 'FINAL'
            AND au.entity_code IN (:entityCodes) AND au.deleted_at IS NULL AND au.acc_type NOT IN ('NEWPR', 'MTCCV') AND
            au.transaction_date < :date::DATE
        """
    )
    suspend fun getOpeningAndClosingLedger(accMode: AccMode, organizationId: String, entityCodes: List<Int>, date: LocalDate?): CreditDebitBalance

    @NewSpan
    @Query(
        """
            with z as (select 
            aau.organization_id,
            entity_code,
            led_currency,
            COALESCE(sum(
                CASE WHEN (acc_type::varchar in (:invoiceAccType)
                    and (due_date >= now()::date)) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_not_due_amount,       
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType)
                    and(now()::date - due_date)  BETWEEN 0 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_thirty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType)
                    and(now()::date - due_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_sixty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType)
                    and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_ninety_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType)
                    and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_one_eighty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType)
                    and(now()::date - due_date) BETWEEN 181 AND 365  THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_three_sixty_five_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType)
                    and(now()::date - due_date) > 365 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_three_sixty_five_plus_amount,
            sum(
                CASE WHEN due_date >= now()::date AND acc_type::varchar in (:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_not_due_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 0 AND 30 AND acc_type::varchar in (:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_thirty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 31 AND 60 AND acc_type::varchar in (:invoiceAccType)  AND (amount_loc - pay_loc) > 0 THEN
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
                CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 AND acc_type::varchar in (:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
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
                        COALESCE(sum(
                CASE WHEN (acc_type::varchar in (:creditNoteAccType)
                    and(due_date >= now()::date)) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS credit_note_not_due_amount,       
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:creditNoteAccType)
                    and(now()::date - due_date)  BETWEEN 0 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS credit_note_thirty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:creditNoteAccType)
                    and(now()::date - due_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS credit_note_sixty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:creditNoteAccType)
                    and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS credit_note_ninety_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in(:creditNoteAccType)
                    and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS credit_note_one_eighty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in(:creditNoteAccType)
                    and(now()::date - due_date) BETWEEN 181 AND 365  THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS credit_note_three_sixty_five_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in(:creditNoteAccType)
                    and(now()::date - due_date) > 365 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS credit_note_three_sixty_five_plus_amount,
            sum(
                CASE WHEN due_date >= now()::date AND acc_type::varchar in (:creditNoteAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_not_due_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 0 AND 30 AND acc_type::varchar in (:creditNoteAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_thirty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 31 AND 60 AND acc_type::varchar in (:creditNoteAccType)  AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_sixty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 61 AND 90 AND acc_type::varchar in (:creditNoteAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_ninety_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 AND acc_type::varchar in(:creditNoteAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_one_eighty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 181 AND 365 AND acc_type::varchar in (:creditNoteAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_three_sixty_five_count,
                sum(
                CASE WHEN (now()::date - due_date) > 365 AND acc_type::varchar in(:creditNoteAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_three_sixty_five_plus_count,
            sum(
                CASE WHEN (now()::date - due_date) > 365 AND acc_type::varchar in(:invoiceAccType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_three_sixty_five_plus_count,
            COALESCE(sum(
                CASE WHEN (acc_type::varchar in (:onAccountAccountType)
                    and(transaction_date >= now()::date)) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_not_due_amount, 
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - transaction_date) BETWEEN 0 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_thirty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - transaction_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_sixty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - transaction_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_ninety_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType) AND
                    (now()::date - transaction_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_one_eighty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - transaction_date) BETWEEN 181 AND 365  THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_three_sixty_five_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType)
                    and(now()::date - transaction_date) > 365 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_three_sixty_five_plus_amount,
            sum(
                CASE WHEN transaction_date >= now()::date AND acc_type::varchar in (:onAccountAccountType)  AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_not_due_count,

            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 0 AND 30 AND acc_type::varchar in (:onAccountAccountType) AND  (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_thirty_count,
            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 31 AND 60 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_sixty_count,
            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 61 AND 90 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_ninety_count,
            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 91 AND 180 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_one_eighty_count,
            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 181 AND 365 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_three_sixty_five_count,
            sum(
                CASE WHEN (now()::date - transaction_date) > 365 AND acc_type::varchar in (:onAccountAccountType) AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_three_sixty_five_plus_count,
            COALESCE(sum(
            CASE WHEN acc_type::varchar in (:invoiceAccType) THEN
                sign_flag * (amount_loc - pay_loc)
            ELSE
                0
            END), 0) AS total_open_invoice_amount,
            COALESCE(sum(
            CASE WHEN acc_type::varchar in (:onAccountAccountType) THEN
                sign_flag * (amount_loc - pay_loc)
            ELSE
                0
            END), 0) AS total_open_on_account_amount, 
            COALESCE(sum(
            CASE WHEN acc_type::varchar in (:creditNoteAccType) THEN
                sign_flag * (amount_loc - pay_loc)
            ELSE
                0
            END), 0) AS total_open_credit_note_amount
    from account_utilizations aau 
    WHERE 
        acc_mode::VARCHAR = :accMode
        AND deleted_at IS NULL
        AND acc_type::VARCHAR IN (:accTypes) and acc_type::VARCHAR != 'NEWPR'
        AND document_status IN ('FINAL')
        and (COALESCE(:entityCodes) IS NULL OR entity_code IN (:entityCodes))
        and (COALESCE(:orgIds) IS NULL OR organization_id IN (:orgIds))
    GROUP BY 
        aau.organization_id, entity_code, led_currency)
        select 
        z.*,
        invoice_not_due_amount + on_account_not_due_amount + credit_note_not_due_amount as not_due_outstanding, 
        invoice_thirty_amount + on_account_thirty_amount + credit_note_thirty_amount as thirty_outstanding,
        invoice_sixty_amount + on_account_sixty_amount + credit_note_sixty_amount as sixty_outstanding,
        invoice_ninety_amount + on_account_ninety_amount +  credit_note_ninety_amount as ninety_outstanding, 
        invoice_one_eighty_amount + on_account_one_eighty_amount +  credit_note_one_eighty_amount as  one_eighty_outstanding,
        invoice_three_sixty_five_amount + on_account_three_sixty_five_plus_amount + credit_note_three_sixty_five_amount as three_sixty_five_outstanding,
        invoice_three_sixty_five_plus_amount + on_account_three_sixty_five_plus_amount + credit_note_three_sixty_five_plus_amount as three_sixty_five_plus_outstanding,
        total_open_invoice_amount + total_open_on_account_amount + total_open_credit_note_amount AS total_outstanding
        from z
    """
    )
    suspend fun getArOutstandingData(
        accTypes: List<String>,
        invoiceAccType: List<String>,
        creditNoteAccType: List<String>,
        onAccountAccountType: List<String>,
        orgIds: List<UUID?>?,
        entityCodes: List<Int?>?,
        accMode: String
    ): List<ArOutstandingData>?

    @NewSpan
    @Query(
        """
         SELECT 
            CASE 
                WHEN amount_curr = pay_curr THEN 'PAID'
                WHEN pay_curr = 0 THEN 'UNPAID'
                WHEN amount_curr - pay_curr <> 0 AND pay_curr > 0 THEN 'PARTIAL_PAID'
            END AS status,
            document_no, organization_name, document_value, currency,amount_curr, amount_loc, pay_curr, pay_loc, due_date, transaction_date, entity_code, service_type, acc_type 
        FROM 
            account_utilizations 
        WHERE 
            organization_id =  :organizationId
            AND (amount_curr - pay_curr > 0)
            AND acc_type != 'NEWPR'
            AND acc_mode = 'AR' 
            AND document_status = 'FINAL'  
            AND deleted_at IS NULL

        """
    )
    suspend fun getOrgDetails(organizationId: UUID): List<OpenInvoiceDetails>
}
