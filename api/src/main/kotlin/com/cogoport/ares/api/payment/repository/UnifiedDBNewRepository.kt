package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.common.AresConstants
import com.cogoport.ares.api.common.models.ARLedgerJobDetailsResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.entity.LedgerSummary
import com.cogoport.ares.api.payment.entity.SupplierLevelData
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
            FROM ares.account_utilizations au 
            WHERE au.acc_mode = :accMode AND au.organization_id = :organizationId::UUID AND document_status = 'FINAL'
            AND au.entity_code IN (:entityCodes) AND au.deleted_at IS NULL AND au.acc_type NOT IN ('NEWPR', 'MTCCV') AND
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
    @NewSpan
    @Query(
        """
            with z as (select 
            null as id,
            aau.organization_id,
            entity_code,
            led_currency,
            max(organization_name) as organization_name,
            acc_mode,
            COALESCE(sum(
                CASE WHEN (acc_type::varchar in (:invoiceAccType)
                    and(due_date >= now()::date) AND aau.created_at >= '2023-04-01') THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_not_due_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType)
                    and (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 AND aau.created_at >= '2023-04-01' THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_today_amount,        
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType) AND aau.created_at >= '2023-04-01'
                    and(now()::date - due_date)  BETWEEN 1 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_thirty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType) AND aau.created_at >= '2023-04-01'
                    and(now()::date - due_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_sixty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:invoiceAccType) AND aau.created_at >= '2023-04-01'
                    and(now()::date - due_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_ninety_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType) AND aau.created_at >= '2023-04-01'
                    and(now()::date - due_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_one_eighty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType) AND aau.created_at >= '2023-04-01'
                    and(now()::date - due_date) BETWEEN 181 AND 365  THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_three_sixty_five_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in(:invoiceAccType) AND aau.created_at >= '2023-04-01'
                    and(now()::date - due_date) > 365 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS invoice_three_sixty_five_plus_amount,
            sum(
                CASE WHEN due_date >= now()::date AND acc_type::varchar in (:invoiceAccType) AND aau.created_at >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_not_due_count,
            sum(
                CASE WHEN (now()::date - due_date) >= 0 AND (now()::date - due_date) < 1 AND acc_type::varchar in (:invoiceAccType) AND aau.created_at >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_today_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 1 AND 30 AND acc_type::varchar in (:invoiceAccType) AND aau.created_at >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_thirty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 31 AND 60 AND acc_type::varchar in (:invoiceAccType) AND aau.created_at >= '2023-04-01'  AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_sixty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 61 AND 90 AND acc_type::varchar in (:invoiceAccType) AND aau.created_at >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_ninety_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 91 AND 180 AND acc_type::varchar in(:invoiceAccType) AND aau.created_at >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_one_eighty_count,
            sum(
                CASE WHEN (now()::date - due_date) BETWEEN 181 AND 365 AND acc_type::varchar in (:invoiceAccType) AND aau.created_at >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_three_sixty_five_count,
            sum(
                CASE WHEN (now()::date - due_date) > 365 AND acc_type::varchar in(:invoiceAccType) AND aau.created_at >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS invoice_three_sixty_five_plus_count,
            sum(
                CASE WHEN (acc_type::varchar in (:creditNoteAccType)) AND aau.created_at >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS credit_note_count,
            COALESCE(sum(
                CASE WHEN (acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'
                    and(transaction_date >= now()::date)) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_not_due_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'
                    and (now()::date - transaction_date) >= 0 AND (now()::date - due_date) < 1 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_today_amount,        
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'
                    and(now()::date - transaction_date) BETWEEN 1 AND 30 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_thirty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'
                    and(now()::date - transaction_date) BETWEEN 31 AND 60 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_sixty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'
                    and(now()::date - transaction_date) BETWEEN 61 AND 90 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_ninety_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'
                    and(now()::date - transaction_date) BETWEEN 91 AND 180 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_one_eighty_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'
                    and(now()::date - transaction_date) BETWEEN 181 AND 365  THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_three_sixty_five_amount,
            COALESCE(sum(
                CASE WHEN acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'
                    and(now()::date - transaction_date) > 365 THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS on_account_three_sixty_five_plus_amount,
            sum(
                CASE WHEN transaction_date >= now()::date AND acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'  AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_not_due_count,
            sum(
                CASE WHEN (now()::date - transaction_date) >= 0 AND (now()::date - due_date) < 1 AND acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_today_count,
            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 1 AND 30 AND acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01' AND  (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_thirty_count,
            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 31 AND 60 AND acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_sixty_count,
            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 61 AND 90 AND acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_ninety_count,
            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 91 AND 180 AND acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_one_eighty_count,
            sum(
                CASE WHEN (now()::date - transaction_date) BETWEEN 181 AND 365 AND acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01' AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_three_sixty_five_count,
            sum(
                CASE WHEN (now()::date - transaction_date) > 365 AND acc_type::varchar in (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01'  AND (amount_loc - pay_loc) > 0 THEN
                    1
                ELSE
                    0
                END) AS on_account_three_sixty_five_plus_count,
            COALESCE(sum(
            CASE WHEN aau.created_at >= '2023-04-01' AND acc_type::varchar in (:invoiceAccType) THEN
                sign_flag * (amount_loc - pay_loc)
            ELSE
                0
            END), 0) AS total_open_invoice_amount,
            COALESCE(sum(
                CASE WHEN aau.created_at >= '2023-04-01' AND acc_type::varchar in (:creditNoteAccType) THEN
                    sign_flag * (amount_loc - pay_loc)
                ELSE
                    0
                END), 0) AS total_credit_note_amount,
            COALESCE(sum(
            CASE WHEN aau.transaction_date >= '2023-04-01' AND acc_type::varchar in (:onAccountAccountType) THEN
                sign_flag * (amount_loc - pay_loc)
            ELSE
                0
            END), 0) AS total_open_on_account_amount,
        COALESCE(SUM(
            CASE WHEN (acc_type::VARCHAR IN (:invoiceAccType) or acc_type::VARCHAR IN (:creditNoteAccType) AND aau.created_at >= '2023-04-01' ) THEN
              sign_flag * amount_loc
            ELSE
              0
            END
          ), 0) AS total_invoice_amount,
          COALESCE(SUM(
            CASE WHEN acc_type::VARCHAR IN (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01' THEN
              sign_flag * amount_loc
            ELSE
              0
            END
          ), 0) AS total_on_account_amount,
          SUM(
            CASE WHEN acc_type::VARCHAR IN (:onAccountAccountType) AND aau.transaction_date >= '2023-04-01' THEN
              1
            ELSE
              0
            END
          ) AS total_on_account_count,
          SUM(
            CASE WHEN (acc_type::VARCHAR IN (:invoiceAccType) or acc_type::VARCHAR IN (:creditNoteAccType)) AND aau.created_at >= '2023-04-01' THEN
              1
            ELSE
              0
            END
          ) AS total_invoices_count,
        CURRENT_DATE AS created_at,
        otpd.registration_number,
        soim.sage_organization_id as bpr,
        COALESCE(cb.closing_balance_debit_2022, 0) as closing_on_account_balance2022,
        COALESCE(cb.closing_balance_credit_2022, 0) as closing_invoice_balance2022,
        COALESCE(cb.closing_balance_debit_2022 - cb.closing_balance_credit_2022, 0) as closing_outstanding2022
    from ares.account_utilizations aau 
    left join organization_trade_party_details otpd on otpd.id = aau.organization_id and otpd.status = 'active'
    left join sage_organization_id_mappings soim on soim.trade_party_detail_serial_id::varchar = otpd.serial_id::varchar and soim.status = 'active' and account_type = 'service_provider'
    left join ares.closing_balances cb on cb.organization_id = otpd.id and cb.registration_number = otpd.registration_number and soim.sage_organization_id::VARCHAR = cb.sage_organization_id::varchar
    WHERE 
        acc_mode::VARCHAR = :accMode
        AND deleted_at IS NULL
        AND acc_type::VARCHAR IN (:accTypes) and acc_type::VARCHAR != 'NEWPR'
        AND document_status IN ('FINAL')
    GROUP BY 
        aau.organization_id, entity_code, led_currency, acc_mode, otpd.registration_number, soim.sage_organization_id, cb.closing_balance_debit_2022, cb.closing_balance_credit_2022)
        select 
        z.*,
        invoice_not_due_amount + on_account_not_due_amount as not_due_outstanding, 
        on_account_today_amount + invoice_today_amount as today_outstanding,
        invoice_thirty_amount + on_account_thirty_amount as thirty_outstanding,
        invoice_sixty_amount + on_account_sixty_amount as sixty_outstanding,
        invoice_ninety_amount + on_account_ninety_amount as ninety_outstanding, 
        invoice_one_eighty_amount + on_account_one_eighty_amount as  one_eighty_outstanding,
        invoice_three_sixty_five_amount + on_account_three_sixty_five_plus_amount as three_sixty_five_outstanding,
        invoice_three_sixty_five_plus_amount + on_account_three_sixty_five_plus_amount as three_sixty_five_plus_outstanding,
        total_open_invoice_amount + total_open_on_account_amount AS total_outstanding
        from z
        """
    )
    suspend fun getLedgerSummaryForAp(
        accTypes: List<String>,
        accMode: String,
        invoiceAccType: List<String>,
        onAccountAccountType: List<String>,
        creditNoteAccType: List<String>
    ): List<LedgerSummary>?

    @NewSpan
    @Query(
        """
            WITH x AS (
                SELECT *
                FROM ares.ledger_summary
                WHERE acc_mode = 'AP'
            ), y AS (
                SELECT
                    otpd.id AS organization_id,
                    otpd.registration_number,
                    json_agg(DISTINCT trade_party_type) AS trade_type,
                    o.serial_id AS organization_serial_id,
                    o.id as self_organization_id,
                    json_agg(json_build_object(
                        'id', u.id,
                        'name', u.name,
                        'email', u.email,
                        'mobileCountryCode', u.mobile_country_code,
                        'mobile_number', u.mobile_number,
                        'stakeholder_type', os.stakeholder_type
                    ))::VARCHAR AS agent,
                    COALESCE((SELECT free_credit_days FROM organization_payment_modes WHERE organization_id = o.id AND organization_trade_party_id = otpd.id limit 1), 0) AS credit_days,
                    o.company_type,
                    otpd.serial_id AS trade_party_serial_id,
                    o.country_id,
                    (SELECT country_code FROM locations WHERE country_id = o.country_id LIMIT 1) AS country_code
                FROM organization_trade_party_details otpd
                INNER JOIN organization_trade_parties otp ON otp.organization_trade_party_detail_id = otpd.id AND otp.status = 'active'
                INNER JOIN organizations o ON o.id = otp.organization_id AND o.status = 'active' AND o.account_type = 'service_provider' AND otpd.registration_number = o.registration_number
                INNER JOIN organization_stakeholders os ON os.organization_id = o.id
                INNER JOIN users u ON u.id = os.stakeholder_id
                WHERE otpd.status = 'active'
                GROUP BY o.serial_id, o.company_type, otpd.id, otpd.registration_number, otpd.serial_id, o.country_id, o.id
            )
            SELECT x.*,
                   y.trade_type::VARCHAR,
                   y.organization_serial_id,
                   y.credit_days,
                   y.country_id,
                   y.agent,
                   y.company_type,
                   y.trade_party_serial_id,
                   y.country_code,
                   y.self_organization_id
            FROM x
            LEFT JOIN y ON x.organization_id = y.organization_id AND x.registration_number = y.registration_number
        """
    )
    suspend fun getSupplierDetailData(): List<SupplierLevelData>
}
