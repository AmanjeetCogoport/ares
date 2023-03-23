package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.CustomerOutstandingPaymentResponse
import com.cogoport.ares.api.payment.model.PaymentUtilizationResponse
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.math.BigDecimal
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface AccountUtilizationRepo : CoroutineCrudRepository<AccountUtilization, Long> {
    @NewSpan
    @Query(
        """
            UPDATE account_utilizations SET 
            tagged_settlement_id = CASE WHEN tagged_settlement_id IS NULL THEN :taggedSettlementIds
                                        ELSE CONCAT(tagged_settlement_id, ', ',:taggedSettlementIds)
                                   END
            WHERE document_no = :documentNo
            and acc_mode = 'AP' and is_void = false and deleted_at is null
        """
    )
    suspend fun updateTaggedSettlementIds(documentNo: Long, taggedSettlementIds: String)

    @NewSpan
    @Query(
        """select account_utilizations.id,document_no,document_value , zone_code,service_type,document_status,entity_code , category,org_serial_id,sage_organization_id
           ,organization_id, tagged_organization_id, trade_party_mapping_id, organization_name,acc_code,acc_type,account_utilizations.acc_mode,sign_flag,currency,led_currency,amount_curr, amount_loc,pay_curr
           ,pay_loc,due_date,transaction_date,created_at,updated_at, taxable_amount, migrated, is_void,tagged_settlement_id,  tds_amount, tds_amount_loc
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
              updated_at = NOW(), is_void = :isVoid WHERE id in (:ids) AND deleted_at is null"""
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
    suspend fun updatePayableAmount(documentNo: Long, tdsAmount: BigDecimal, tdsAmountLoc: BigDecimal)

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
            SELECT id,pay_curr,pay_loc FROM account_utilizations WHERE document_no = :paymentNum AND acc_mode = 'AP'
        """
    )
    suspend fun getDataByPaymentNumForTaggedBill(paymentNum: Long?): PaymentUtilizationResponse

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
                    AND deleted_at IS NULL
                GROUP BY
                    organization_id) AS c
        """
    )
    suspend fun getInvoicesOutstandingAgeingBucketCount(queryName: String?, orgId: String?): Int
}
