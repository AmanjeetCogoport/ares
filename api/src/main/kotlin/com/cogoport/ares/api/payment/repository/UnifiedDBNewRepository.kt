package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.ARLedgerJobDetailsResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.model.common.TradePartyOutstandingRes
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.response.CreditDebitBalance
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import io.micronaut.transaction.annotation.TransactionalAdvice
import java.time.LocalDate
import java.util.UUID

@TransactionalAdvice(AresConstants.UNIFIED)
@R2dbcRepository(value = AresConstants.UNIFIED, dialect = Dialect.POSTGRES)
interface UnifiedDBNewRepository : CoroutineCrudRepository<AccountUtilization, Long> {
    @NewSpan
    @Query(
        """
          WITH shipment_documents_on_document_level AS
            (SELECT shipment_id, document_type,
            CASE WHEN sd.document_type IN ('airway_bill', 'draft_airway_bill', 'bill_of_lading', 'draft_bill_of_lading')
            THEN (array_agg(COALESCE(sd.data -> 'document_number', sd.data -> 'bl_number')))[1] END AS shipment_document_number,
            CASE WHEN sd.document_type IN ('house_bill_of_lading', 'draft_house_bill_of_lading', 'house_airway_bill', 'draft_house_airway_bill')
            THEN (array_agg(COALESCE(sd.data -> 'document_number', sd.data -> 'bl_number')))[1] END AS house_document_number
            FROM shipment_documents sd WHERE sd.state = 'document_accepted' AND
            sd.document_type in ('airway_bill', 'draft_airway_bill', 'bill_of_lading', 'draft_bill_of_lading',
            'house_bill_of_lading', 'draft_house_bill_of_lading', 'house_airway_bill', 'draft_house_airway_bill')
            GROUP BY shipment_id, document_type),
          grouped_shipment_documents AS
            (SELECT shipment_id, (array_remove(array_agg(shipment_document_number), NULL))[1] AS shipment_document_number, (array_remove(array_agg(house_document_number), NULL))[1] AS house_document_number
            FROM shipment_documents_on_document_level
            WHERE (shipment_document_number IS NOT NULL OR house_document_number IS NOT NULL)
            GROUP BY shipment_id)
          SELECT au.transaction_date::varchar AS transaction_date,
            au.acc_type as document_type,
            au.document_value::varchar AS document_number,
            au.currency as currency,
            au.amount_curr::varchar AS amount,
            CASE WHEN au.sign_flag = -1 THEN au.amount_loc ELSE 0 END AS credit,
            CASE WHEN au.sign_flag = 1 THEN au.amount_loc ELSE 0 END AS debit,
            p.trans_ref_number AS transaction_ref_number,
            gsd.shipment_document_number,
            gsd.house_document_number
            FROM ares.account_utilizations au
            LEFT JOIN ares.payments p ON p.payment_num = au.document_no AND p.payment_num_value = au.document_value
            LEFT JOIN plutus.invoices i ON i.invoice_number = au.document_value::varchar AND i.id = au.document_no
            LEFT JOIN loki.jobs j ON j.id = i.job_id
            LEFT JOIN grouped_shipment_documents gsd ON gsd.shipment_id::varchar = j.reference_id
            WHERE au.acc_mode = :accMode AND au.organization_id = :organizationId::UUID AND document_status = 'FINAL'
            AND au.transaction_date >= :startDate::DATE AND au.transaction_date <= :endDate::DATE AND au.entity_code IN (:entityCodes)
            AND au.deleted_at IS NULL AND au.acc_type != 'NEWPR' AND p.deleted_at IS NULL
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
            FROM ares.account_utilizations au 
            WHERE au.acc_mode = :accMode AND au.organization_id = :organizationId::UUID AND document_status = 'FINAL'
            AND au.entity_code IN (:entityCodes) AND au.deleted_at IS NULL AND au.acc_type != 'NEWPR' AND
            au.transaction_date < :date::DATE
        """
    )
    suspend fun getOpeningAndClosingLedger(accMode: AccMode, organizationId: String, entityCodes: List<Int>, date: LocalDate?, commonRow: String): CreditDebitBalance

    @NewSpan
    @Query(
        """
            select au.organization_id::varchar,
            au.entity_code,
            otpd.registration_number,
            (array_agg(DISTINCT (au.led_currency)))[1] AS led_currency,
            sum(case when au.acc_type in ('SINV', 'SREIMB', 'SCN', 'SREIMBCN') and au.amount_curr - au.pay_curr <> 0 and au.document_status = 'FINAL' then 1 else 0 end) as open_invoices_count,
            sum(case when au.acc_type in ('SINV', 'SREIMB', 'SCN', 'SREIMBCN') and au.document_status = 'FINAL' then au.sign_flag * (au.amount_loc - au.pay_loc)  else 0 end) as open_invoices_led_amount,
            sum(case when au.acc_type in ('SINV', 'SREIMB', 'SCN', 'SREIMBCN') and au.document_status = 'FINAL' AND au.due_date < now()::date then au.sign_flag * (au.amount_loc - au.pay_loc) else 0 end) as overdue_open_invoices_led_amount,
            sum(case when au.acc_type in ('SINV', 'SREIMB', 'SCN', 'SREIMBCN', 'REC', 'CTDS', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'PAY') and au.document_status = 'FINAL' then au.sign_flag * (au.amount_loc - au.pay_loc) else 0 end) as outstanding_led_amount
            from ares.account_utilizations au
            inner join  organization_trade_party_details otpd on au.organization_id = otpd.id
            where au.acc_type in ('SINV','SCN','REC', 'CTDS', 'SREIMB', 'SREIMBCN', 'BANK', 'CONTR', 'ROFF', 'MTCCV', 'MISC', 'INTER', 'OPDIV', 'MTC', 'PAY') 
            and au.acc_mode = 'AR'
            and au.document_status = 'FINAL'  
            and au.organization_id IN (:orgIds) 
            and (COALESCE(:entityCodes) IS NULL OR au.entity_code IN (:entityCodes))
            and au.deleted_at is null
            group by au.organization_id, au.entity_code, otpd.registration_number
        """
    )
    suspend fun getTradePartyOutstanding(orgIds: List<UUID>, entityCodes: List<Int>): List<TradePartyOutstandingRes>?
}
