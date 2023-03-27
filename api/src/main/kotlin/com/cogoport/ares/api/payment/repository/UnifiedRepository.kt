package com.cogoport.ares.api.payment.repository

import com.cogoport.ares.api.payment.entity.BfCustomerProfitabilityResp
import com.cogoport.ares.api.payment.entity.BfReceivableAndPayable
import com.cogoport.ares.api.payment.entity.BfShipmentProfitabilityResp
import com.cogoport.ares.api.payment.entity.LogisticsMonthlyData
import com.cogoport.ares.api.payment.entity.PaymentFile
import com.cogoport.ares.api.payment.entity.ProfitCountResp
import com.cogoport.ares.api.payment.entity.TodayPurchaseStats
import com.cogoport.ares.api.payment.entity.TodaySalesStat
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.ServiceType
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import io.micronaut.transaction.annotation.TransactionalAdvice
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date

@TransactionalAdvice("dms")
@R2dbcRepository(value = "dms", dialect = Dialect.POSTGRES)
interface UnifiedRepository : CoroutineCrudRepository<PaymentFile, Long> {

    @NewSpan
    @Query(
        """
            SELECT
		sum(
			CASE WHEN au.due_date >= now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS non_overdue_amount,
		sum(
			CASE WHEN au.due_date < now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS overdue_amount,
		sum(
			CASE WHEN ((au.amount_loc - au.pay_loc) > 0
				AND au.acc_type IN ('SINV','SREIMB')) THEN
				1
			ELSE 0 END) AS not_paid_document_count,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 1 AND 30 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS thirty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 31 AND 60 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS sixty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 61 AND 90 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS ninety_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 91 AND 180 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS one_eighty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 181 AND 360 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS three_sixty_day_overdue,
        sum(
			CASE WHEN (now()::date - au.due_date) > 360 THEN
				sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS three_sixty_plus_day_overdue,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('import','IMPORT')
               AND au.service_type in (:oceanServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_ocean_import_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('export','EXPORT')
            AND au.service_type in (:oceanServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_ocean_export_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('export','EXPORT')
           AND au.service_type in (:airServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_air_export_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('import','IMPORT')
             AND au.service_type in (:airServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_air_import_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('domestic','LOCAL')
           AND au.service_type in (:airServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_air_others_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('domestic')
            AND au.service_type in (:surfaceServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_surface_domestic_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('LOCAL')
            AND au.service_type in (:surfaceServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_surface_local_due
	FROM
		ares.account_utilizations au JOIN 
        plutus.invoices iv ON au.document_no = iv.id JOIN
        loki.jobs j on j.id = iv.job_id 
	WHERE
		au.acc_mode = 'AR'
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
		AND au.deleted_at IS NULL
		AND au.acc_type IN ('SINV','SCN','SREIMB')
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (COALESCE(:customerIds) is null or au.tagged_organization_id::varchar in (:customerIds))
         AND (:entityCode is null or au.entity_code = :entityCode)
        AND (:startDate is null or :endDate is null or iv.invoice_date::DATE BETWEEN :startDate AND :endDate)
        AND (COALESCE(:tradeType) is null or j.job_details->>'tradeType' in (:tradeType))
        """
    )
    fun getBfReceivable(
        serviceTypes: List<ServiceType>?,
        startDate: Date?,
        endDate: Date?,
        tradeType: List<String>?,
        customerIds: List<String>?,
        entityCode: Int?,
        oceanServices: List<ServiceType>?,
        airServices: List<ServiceType>?,
        surfaceServices: List<ServiceType>?
    ): BfReceivableAndPayable

    @NewSpan
    @Query(
        """
            SELECT
	o.id
FROM
	organizations o
	JOIN lead_organization_segmentations los ON los.lead_organization_id = o.lead_organization_id
WHERE
	los.segment in(:customerTypes)
        """
    )
    fun getCustomerIds(customerTypes: List<String>): List<String>

    @NewSpan
    @Query(
        """
            SELECT
		sum(
			CASE WHEN au.due_date >= now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS non_overdue_amount,
		sum(
			CASE WHEN au.due_date < now()::date THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS overdue_amount,
		sum(
			CASE WHEN ((au.amount_loc - au.pay_loc) > 0
				AND au.acc_type IN ('PINV','PREIMB')) THEN
				1
			ELSE 0 END) AS not_paid_document_count,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 1 AND 30 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS thirty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 31 AND 60 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS sixty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 61 AND 90 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS ninety_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 91 AND 180 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS one_eighty_day_overdue,
		sum(
			CASE WHEN (now()::date - au.due_date) BETWEEN 181 AND 360 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS three_sixty_day_overdue,
        sum(
			CASE WHEN (now()::date - au.due_date) > 360 THEN
				au.sign_flag * (au.amount_loc - au.pay_loc)
			ELSE 0 END) AS three_sixty_plus_day_overdue,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('import','IMPORT')
            AND au.service_type in (:oceanServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_ocean_import_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('export','EXPORT')
            AND au.service_type in (:oceanServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_ocean_export_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('export','EXPORT')
           AND au.service_type in (:airServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_air_export_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('import','IMPORT')
            AND au.service_type in (:airServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_air_import_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('domestic','LOCAL')
            AND au.service_type in (:airServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_air_others_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('domestic')
             AND au.service_type in (:surfaceServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_surface_domestic_due,
        sum(
            CASE WHEN j.job_details->>'tradeType' IN ('LOCAL')
            AND au.service_type in (:surfaceServices) THEN
            au.sign_flag * (au.amount_loc - au.pay_loc) ELSE 0 END) AS total_surface_local_due
	FROM
		ares.account_utilizations au JOIN 
        kuber.bills bill ON au.document_no = bill.id JOIN
        loki.jobs j on j.id = bill.job_id
	WHERE
		au.acc_mode = 'AP'
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
		AND au.deleted_at IS NULL
		AND au.acc_type IN ('PINV','PCN','PREIMB')
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (:entityCode is null or au.entity_code = :entityCode)
        AND (:startDate is null or :endDate is null or bill.bill_date::DATE BETWEEN :startDate::DATE AND :endDate::DATE)
        AND (COALESCE(:tradeType) is null or j.job_details->>'tradeType' in (:tradeType))
        """
    )
    fun getBfPayable(
        serviceTypes: List<ServiceType>?,
        startDate: Date?,
        endDate: Date?,
        tradeType: List<String>?,
        entityCode: Int?,
        oceanServices: List<ServiceType>?,
        airServices: List<ServiceType>?,
        surfaceServices: List<ServiceType>?
    ): BfReceivableAndPayable

    @NewSpan
    @Query(
        """
            SELECT
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:endYear, '-01-01')::DATE
			AND CONCAT(:endYear, '-01-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS january,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:endYear, '-02-01')::DATE
			AND CONCAT(:endYear, '-02-28')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS february,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:endYear, '-03-01')::DATE
			AND CONCAT(:endYear, '-03-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS march,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-04-01')::DATE
			AND CONCAT(:startYear, '-04-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS april,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-05-01')::DATE
			AND CONCAT(:startYear, '-05-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS may,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-06-01')::DATE
			AND CONCAT(:startYear, '-06-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS june,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-07-01')::DATE
			AND CONCAT(:startYear, '-07-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
               CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS july,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-08-01')::DATE
			AND CONCAT(:startYear, '-08-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS august,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-09-01')::DATE
			AND CONCAT(:startYear, '-09-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS september,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-10-01')::DATE
			AND CONCAT(:startYear, '-10-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS october,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-11-01')::DATE
			AND CONCAT(:startYear, '-11-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS november,
	sum(
		CASE WHEN invoice_date BETWEEN CONCAT(:startYear, '-12-01')::DATE
			AND CONCAT(:startYear, '-12-30')::DATE THEN
            CASE WHEN inv.invoice_type = 'INVOICE' THEN
                CASE WHEN :isPostTax = TRUE THEN inv.ledger_total ELSE inv.sub_total END
                WHEN inv.invoice_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * inv.ledger_total ELSE -1 * inv.sub_total END
            ELSE 0 END
		ELSE 0 END) AS december
FROM
	plutus.invoices inv
	INNER JOIN ares.account_utilizations au ON au.document_no = inv.id
		AND au.acc_mode = 'AR'
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes))
        AND au.document_status = 'FINAL'
        AND au.entity_code IN ('101','301')
        AND inv.status NOT IN ('DRAFT','FINANCE_REJECTED','IRN_CANCELLED','CONSOLIDATED')
        """
    )
    fun getBfIncomeMonthly(serviceTypes: List<ServiceType>?, startYear: String, endYear: String, isPostTax: Boolean): LogisticsMonthlyData

    @NewSpan
    @Query(
        """
            SELECT
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:endYear, '-01-01')::DATE
			AND CONCAT(:endYear, '-01-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                 CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS january,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:endYear, '-02-01')::DATE
			AND CONCAT(:endYear, '-02-28')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS february,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:endYear, '-03-01')::DATE
			AND CONCAT(:endYear, '-03-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS march,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:startYear, '-04-01')::DATE
			AND CONCAT(:startYear, '-04-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS april,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:startYear, '-05-01')::DATE
			AND CONCAT(:startYear, '-05-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS may,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:startYear, '-06-01')::DATE
			AND CONCAT(:startYear, '-06-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS june,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:startYear, '-07-01')::DATE
			AND CONCAT(:startYear, '-07-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS july,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:startYear, '-08-01')::DATE
			AND CONCAT(:startYear, '-08-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS august,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:startYear, '-09-01')::DATE
			AND CONCAT(:startYear, '-09-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS september,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:startYear, '-10-01')::DATE
			AND CONCAT(:startYear, '-10-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS october,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:startYear, '-11-01')::DATE
			AND CONCAT(:startYear, '-11-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS november,
	sum(
		CASE WHEN bill_date BETWEEN CONCAT(:startYear, '-12-01')::DATE
			AND CONCAT(:startYear, '-12-30')::DATE THEN
            CASE WHEN bill.bill_type = 'BILL' THEN
                CASE WHEN :isPostTax = TRUE THEN bill.ledger_total ELSE bill.sub_total END
                WHEN bill.bill_type = 'CREDIT_NOTE' THEN
                CASE WHEN :isPostTax = TRUE THEN - 1 * bill.ledger_total ELSE -1 * bill.sub_total END
                ELSE 0 END
		ELSE
			0
		END) AS december
FROM
	kuber.bills bill
	LEFT JOIN ares.account_utilizations au ON au.document_no = bill.id
		where au.acc_mode = 'AP'
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes))
        AND au.document_status = 'FINAL'
        AND au.entity_code IN ('101','301')
        AND bill.status NOT IN ('INITIATED','COE_REJECTED','FINANCE_REJECTED')
        """
    )
    fun getBfExpenseMonthly(serviceTypes: List<ServiceType>?, startYear: String, endYear: String, isPostTax: Boolean): LogisticsMonthlyData

    @NewSpan
    @Query(
        """
    SELECT
	sum(
		CASE WHEN iv.invoice_date::date = :date::date THEN
			CASE WHEN iv.invoice_type = 'INVOICE' THEN
				iv.ledger_total
			WHEN iv.invoice_type = 'CREDIT_NOTE' THEN
				- 1 * iv.ledger_total
			ELSE 0 END
		ELSE 0 END) AS total_revenue,
	sum(
		CASE WHEN iv.invoice_date::date = :date::date
			AND iv.invoice_type = 'INVOICE' THEN
			1
		ELSE 0 END) AS total_invoices,
	count(DISTINCT CASE WHEN au.acc_type = 'SINV' THEN
			au.tagged_organization_id
		ELSE NULL END) AS total_sales_orgs
    FROM
	plutus.invoices iv
	JOIN ares.account_utilizations au ON iv.id = au.document_no
    WHERE
    au.acc_mode = 'AR'
    AND au.acc_type IN ('SINV','SCN')
    AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
    AND (:entityCode is null or au.entity_code = :entityCode)
	AND iv.invoice_date::date = :date::date
    AND iv.status NOT IN ('DRAFT','FINANCE_REJECTED','IRN_CANCELLED','CONSOLIDATED')       
     """
    )
    fun getTodaySalesStats(serviceTypes: List<ServiceType>?, entityCode: Int?, date: LocalDate): TodaySalesStat

    @NewSpan
    @Query(
        """
    SELECT
	sum(
		CASE WHEN bill.bill_date::date = :date::date THEN
			CASE WHEN bill.bill_type = 'BILL' THEN
				bill.ledger_total
			WHEN bill.bill_type = 'CREDIT_NOTE' THEN
				- 1 * bill.ledger_total
			ELSE 0 END
		ELSE 0 END) AS total_expense,
	sum(
		CASE WHEN bill.bill_date::date = :date::date
			AND bill.bill_type = 'BILL' THEN
			1
		ELSE 0 END) AS total_bills,
	count(DISTINCT CASE WHEN au.acc_type = 'PINV' THEN
			au.tagged_organization_id
		ELSE NULL END) AS total_purchase_orgs
    FROM
	kuber.bills bill
	JOIN ares.account_utilizations au ON bill.id = au.document_no
    WHERE
    au.acc_mode = 'AP'
    AND au.acc_type IN ('PINV','PCN')
    AND (:entityCode is null or au.entity_code = :entityCode)
	AND bill.bill_date::date = :date::date	
    AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
	AND bill.status NOT IN ('INITIATED','COE_REJECTED','FINANCE_REJECTED')
     """
    )
    fun getTodayPurchaseStats(serviceTypes: List<ServiceType>?, entityCode: Int?, date: LocalDate): TodayPurchaseStats

    @NewSpan
    @Query(
        """
            SELECT
	j.job_number,
	s.shipment_type,
	o.business_name,
	o.sage_company_id::varchar AS entity,
	s.state shipment_milestone,
	j.income AS income,
	j.expense AS expense,
    j.profit_percent as profitability,
    LOWER(j.state) AS job_status
FROM
	loki.jobs j
	JOIN shipments s ON j.job_number::VARCHAR = s.serial_id::VARCHAR
	JOIN organizations o ON o.id = s.importer_exporter_id
WHERE
	j.income != 0
	AND j.expense != 0
    AND (:query IS NULL OR (o.business_name ILIKE :query OR j.job_number ILIKE :query))
    AND (:entityCode IS NULL OR o.sage_company_id = :entityCode::varchar)
    AND (:jobStatus IS NULL OR j.state = :jobStatus)
    ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                     CASE WHEN :sortBy = 'createdAt' THEN EXTRACT(epoch FROM j.created_at)::numeric
                         WHEN :sortBy = 'profit' THEN j.profit_percent
                    END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                     CASE WHEN :sortBy = 'createdAt' THEN EXTRACT(epoch FROM j.created_at)::numeric
                         WHEN :sortBy = 'profit' THEN j.profit_percent
                    END        
            END 
            Asc
    OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
    """
    )
    fun listShipmentProfitability(page: Int, pageLimit: Int, query: String?, jobStatus: String?, sortBy: String?, sortType: String?, entityCode: Int?): List<BfShipmentProfitabilityResp>

    @NewSpan
    @Query(
        """
    SELECT COUNT(*) AS total_count ,sum(j.profit_percent)/100 AS average_profit
    FROM
	loki.jobs j
	JOIN shipments s ON j.job_number::VARCHAR = s.serial_id::VARCHAR
	JOIN organizations o ON o.id = s.importer_exporter_id
    WHERE
	j.income != 0
	AND j.expense != 0
    AND (:entityCode IS NULL OR o.sage_company_id = :entityCode::varchar)
    AND (:query IS NULL OR (o.business_name ILIKE :query OR j.job_number ILIKE :query))
    AND (:jobStatus IS NULL OR j.state = :jobStatus)     
        """
    )
    fun findTotalCountShipment(query: String?, jobStatus: String?, entityCode: Int?): ProfitCountResp

    @NewSpan
    @Query(
        """
    SELECT
	count(DISTINCT s.serial_id) AS shipment_count,
	s.importer_exporter_id,o.sage_company_id as entity,
	o.business_name,sum(j.income) AS booked_income,sum(j.expense) AS booked_expense,
    (SUM(j.income) - SUM(j.expense)) / 100 as profitability

FROM
	loki.jobs j
	JOIN shipments s ON j.job_number::varchar = s.serial_id::VARCHAR
	JOIN organizations o ON o.id = s.importer_exporter_id
WHERE
	o.account_type = 'importer_exporter'
	AND j.income != 0
	AND j.expense != 0
	AND (:entityCode IS NULL OR o.sage_company_id = :entityCode::varchar)
	AND s.state != 'cancelled'
    AND (:query IS NULL OR o.business_name ILIKE :query)
GROUP BY
	s.importer_exporter_id,
	o.business_name,
	o.sage_company_id
        ORDER BY
            CASE WHEN :sortType = 'Desc' THEN
                     CASE WHEN :sortBy = 'profit' THEN ((SUM(j.income) - SUM(j.expense)) / 100) ELSE random() END
            END 
            Desc,
            CASE WHEN :sortType = 'Asc' THEN
                     CASE WHEN :sortBy = 'profit' THEN ((SUM(j.income) - SUM(j.expense)) / 100) ELSE random() END    
            END 
            Asc
    OFFSET GREATEST(0, ((:page - 1) * :pageLimit)) LIMIT :pageLimit
    """
    )
    fun listCustomerProfitability(page: Int, pageLimit: Int, query: String?, sortBy: String?, sortType: String?, entityCode: Int?): List<BfCustomerProfitabilityResp>

    @NewSpan
    @Query(
        """
             SELECT
             COUNT(DISTINCT s.importer_exporter_id) AS total_count,
             (SUM(j.income) - SUM(j.expense)) / 100 AS average_profit
FROM
	loki.jobs j
	JOIN shipments s ON j.job_number::VARCHAR = s.serial_id::VARCHAR
	JOIN organizations o ON o.id = s.importer_exporter_id
WHERE
	o.account_type = 'importer_exporter'
	AND j.income != 0
	AND j.expense != 0
	AND (:entityCode IS NULL OR o.sage_company_id = :entityCode::varchar)
	AND s.state != 'cancelled'
    AND (:query IS NULL OR o.business_name ILIKE :query)  
        """
    )
    fun findTotalCountCustomer(query: String?, entityCode: Int?): ProfitCountResp

    @Query(
        """
             SELECT
		sum(au.sign_flag * (au.amount_loc - au.pay_loc)) 
	FROM
		ares.account_utilizations au 
	WHERE
		au.acc_mode = :accMode
		AND au.due_date IS NOT NULL
		AND au.document_status in('FINAL')
		AND au.deleted_at IS NULL
		AND au.acc_type IN (:accType)
        AND (COALESCE(:serviceTypes) is null or au.service_type in (:serviceTypes)) 
        AND (:entityCode IS NULL OR au.entity_code = :entityCode)
        """
    )
    fun getTotalRemainingAmount(accMode: AccMode, accType: List<AccountType>, serviceTypes: List<ServiceType>, entityCode: Int?): BigDecimal?
}
