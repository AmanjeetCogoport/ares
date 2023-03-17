package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.AccountUtilization
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan

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
            and acc_mode = 'AP' and is_draft = false and deleted_at is null
        """
    )
    suspend fun updateTaggedSettlementIds(documentNo: Long, taggedSettlementIds: String)

    @NewSpan
    @Query(
        """select account_utilizations.id,document_no,document_value , zone_code,service_type,document_status,entity_code , category,org_serial_id,sage_organization_id
           ,organization_id, tagged_organization_id, trade_party_mapping_id, organization_name,acc_code,acc_type,account_utilizations.acc_mode,sign_flag,currency,led_currency,amount_curr, amount_loc,pay_curr
           ,pay_loc,due_date,transaction_date,created_at,updated_at, taxable_amount, migrated, is_draft,tagged_settlement_id,  payable_amount, payable_amount_loc
            from account_utilizations 
            where document_no in (:documentNo) and acc_type::varchar in (:accType) 
            and (:accMode is null or acc_mode=:accMode::account_mode)
             and account_utilizations.deleted_at is null order by updated_at desc"""
    )
    suspend fun findRecords(documentNo: List<Long>, accType: List<String?>, accMode: String? = null): MutableList<AccountUtilization>

    @NewSpan
    @Query(

        """UPDATE account_utilizations SET 
              updated_at = NOW(), is_draft = :isDraft WHERE id =:id AND deleted_at is null"""
    )
    suspend fun updateAccountUtilizations(id: Long, isDraft: Boolean)
}
