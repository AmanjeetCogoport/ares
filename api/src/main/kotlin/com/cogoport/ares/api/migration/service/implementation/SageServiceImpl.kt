package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.migration.constants.MigrationConstants
import com.cogoport.ares.api.migration.model.GlCodeRecordsManager
import com.cogoport.ares.api.migration.model.InvoiceDetailRecordManager
import com.cogoport.ares.api.migration.model.InvoiceDetails
import com.cogoport.ares.api.migration.model.JVLineItemNoBPR
import com.cogoport.ares.api.migration.model.JVParentDetails
import com.cogoport.ares.api.migration.model.JVParentRecordManger
import com.cogoport.ares.api.migration.model.JVRecordsForSchedulerManager
import com.cogoport.ares.api.migration.model.JVRecordsScheduler
import com.cogoport.ares.api.migration.model.JVRecordsWithoutBprManager
import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.JournalVoucherRecordManager
import com.cogoport.ares.api.migration.model.NewPeriodRecord
import com.cogoport.ares.api.migration.model.NewPeriodRecordManager
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.PaymentRecordManager
import com.cogoport.ares.api.migration.model.SettlementRecord
import com.cogoport.ares.api.migration.model.SettlementRecordManager
import com.cogoport.ares.api.migration.service.interfaces.SageService
import com.cogoport.ares.api.payment.repository.PaymentRepository
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PaymentDetailsInfo
import com.cogoport.ares.model.payment.PaymentNumInfo
import com.cogoport.ares.model.payment.PlatformPostPaymentDetails
import com.cogoport.ares.model.payment.PostPaymentInfo
import com.cogoport.ares.model.payment.SagePaymentNumMigrationResponse
import com.cogoport.ares.model.payment.SagePostPaymentDetails
import com.cogoport.ares.model.settlement.GlCodeMaster
import com.cogoport.brahma.sage.Client
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class SageServiceImpl : SageService {

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    @Inject
    lateinit var paymentRepository: PaymentRepository

    override suspend fun getPaymentDataFromSage(startDate: String?, endDate: String?, bpr: String, mode: String): ArrayList<PaymentRecord> {
        val sqlQuery = """
        SELECT  P.FCY_0 as entity_code 
            ,P.BPR_0 as sage_organization_id 
            ,P.NUM_0 as sage_ref_number
            ,P.BPANAM_0 as organization_name
            ,P.ACC_0 as acc_code
            ,case when P.BPRSAC_0='SC' then 'AP' else 'AR' end as acc_mode
            ,case when P.PAM_0='BNK' then 'BANK'
                  when P.PAM_0='CSH' then 'CASH'
                  else P.PAM_0 end as pay_mode 
            ,P.DES_0 as narration
            ,P.ACCDAT_0 as transaction_date 
            ,G.DUDDAT_0 as due_date
            ,P.CREDATTIM_0 as created_at
            ,P.UPDDATTIM_0 as updated_at
            ,case when P.PAYTYP_0 in('TDSC','TDS') and P.BPRSAC_0='AR' then 'CTDS' 
                  when P.PAYTYP_0 in('TDSC','TDS') and P.BPRSAC_0='SC' then 'VTDS'
                  else P.PAYTYP_0 end as payment_code
            ,G.AMTCUR_0 as account_util_amt_curr
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,P.AMTCUR_0 as amount 
            ,P.AMTCUR_0 * GC.RATMLT_0 as led_amount
            ,P.BANPAYTPY_0 as bank_pay_amount
            ,case G.sign_flag when 0 then 1 else G.sign_flag end as sign_flag
            ,GC.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,GC.RATMLT_0 as exchange_rate
            ,G.TYP_0 as account_type
            ,case when P.BPRSAC_0='AR' then 
            (select XX1P4PANNO_0 from COGO2.BPCUSTOMER where BPCNUM_0=P.BPR_0)
            else (select XX1P4PANNO_0 from COGO2.BPSUPPLIER where BPSNUM_0=P.BPR_0) end as pan_number
            ,P.BAN_0 as bank_short_code
            from COGO2.PAYMENTH P INNER JOIN COGO2.GACCENTRY GC on (P.NUM_0 = GC.REF_0 and GC.FCY_0=P.FCY_0)
            INNER JOIN       
            (
             select NUM_0,TYP_0,FCY_0,SAC_0,BPR_0,DUDDAT_0,PAM_0,SUM(AMTCUR_0) as AMTCUR_0,SIGN(SUM(SNS_0*(AMTLOC_0-PAYLOC_0))) as sign_flag
             ,SUM(AMTLOC_0) as AMTLOC_0,SUM(PAYCUR_0) as PAYCUR_0,SUM(PAYLOC_0) as PAYLOC_0 from  COGO2.GACCDUDATE G where SAC_0 in('AR','SC')
             group by NUM_0,TYP_0,FCY_0,SAC_0,BPR_0,DUDDAT_0,PAM_0
            ) G            
            on (GC.NUM_0 = G.NUM_0 and  G.SAC_0 = P.BPRSAC_0 and G.BPR_0 = P.BPR_0 and G.BPR_0<>'' and G.FCY_0=P.FCY_0)
            where P.BPRSAC_0 = '$mode'  and P.BPR_0 = '$bpr'
            and G.BPR_0 not in ${MigrationConstants.administrativeExpense}
            and P.ACCDAT_0 BETWEEN '$startDate' and '$endDate' order by P.ACCDAT_0 ASC
             """
        //  -- and P.ACCDAT_0 BETWEEN '$startDate' and '$endDate' order by P.ACCDAT_0 ASC
        val paymentRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(paymentRecords, PaymentRecordManager::class.java)
        return payments.recordSets!![0]
    }

    override suspend fun getJournalVoucherFromSage(
        startDate: String?,
        endDate: String?,
        jvNums: String?
    ): ArrayList<JournalVoucherRecord> {
        var sqlQuery = """
         SELECT G.FCY_0 as entity_code 
            ,G.BPR_0 as sage_organization_id 
            ,G.NUM_0 as payment_num
            ,case when G.SAC_0='AR' then 
            (select BPCNAM_0 from COGO2.BPCUSTOMER where BPCNUM_0=G.BPR_0)
            else (select BPSNAM_0 from COGO2.BPSUPPLIER where BPSNUM_0=G.BPR_0) end as organization_name
            ,GD.ACC_0 as acc_code
            ,case when G.SAC_0='SC' then 'AP' else G.SAC_0 end as acc_mode
            ,case when G.PAM_0='BNK' then 'BANK'
                  when G.PAM_0='CSH' then 'CASH'
                  else G.PAM_0 end as pay_mode 
            ,GD.DES_0 as narration
            ,GC.ACCDAT_0 as transaction_date 
            ,G.DUDDAT_0 as due_date
            ,GD.CREDATTIM_0 as created_at
            ,GD.UPDDATTIM_0 as updated_at
            ,GC.RATMLT_0 as exchange_rate
            ,case when G.SAC_0='AR' then 'REC' else 'PAY' end  as payment_code
            ,G.AMTCUR_0 as account_util_amt_curr    
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,case G.sign_flag when 0 then 1 else G.sign_flag end as sign_flag
            ,G.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,G.TYP_0 as account_type
            ,GD.ROWID as sage_unique_id
            from  COGO2.GACCENTRY GC 
            INNER JOIN           
            (
            select TYP_0,NUM_0,FCY_0,CUR_0,SAC_0,BPR_0,DUDDAT_0,PAM_0,SUM(AMTCUR_0) as AMTCUR_0,SUM(AMTLOC_0) as AMTLOC_0,SUM(PAYCUR_0) as PAYCUR_0,SUM(PAYLOC_0) as PAYLOC_0
            ,MAX(SNS_0) as sign_flag
            from  COGO2.GACCDUDATE where SAC_0 in('AR','SC','CSD','PDA','EMD','SUSS','SUSA') and TYP_0 in ('BANK','CONTR','INTER','MISC','MTC','MTCCV','OPDIV') 
            GROUP BY TYP_0,NUM_0,FCY_0,CUR_0,SAC_0,BPR_0,DUDDAT_0,PAM_0, SNS_0
            ) G 
            on (GC.NUM_0 = G.NUM_0 and GC.FCY_0=G.FCY_0)
            INNER JOIN COGO2.GACCENTRYD GD on (GD.NUM_0 = GC.NUM_0 and GD.TYP_0 = G.TYP_0 and GD.SAC_0 = G.SAC_0 and GD.AMTCUR_0 = G.AMTCUR_0 and GD.BPR_0 = G.BPR_0)
            where G.SAC_0 in('AR','SC','CSD','PDA','EMD','SUSS','SUSA') and G.TYP_0 in ('BANK','CONTR','INTER','MISC','MTC','MTCCV','OPDIV')
            and G.BPR_0 not in ${MigrationConstants.administrativeExpense}
            """
        if (startDate == null && endDate == null) {
            sqlQuery += """and G.NUM_0 in ($jvNums) order by GC.ACCDAT_0 ASC"""
        } else {
            sqlQuery += """and GC.ACCDAT_0 BETWEEN '$startDate' and '$endDate' order by GC.ACCDAT_0 ASC"""
        }
        val journalRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(journalRecords, JournalVoucherRecordManager::class.java)
        return payments.recordSets!![0]
    }

    override suspend fun migratePaymentsByDate(startDate: String?, endDate: String?, updatedAt: String?): ArrayList<PaymentRecord> {
        var sqlQuery =
            """
            SELECT  P.FCY_0 as entity_code 
            ,P.BPR_0 as sage_organization_id 
            ,P.NUM_0 as sage_ref_number
            ,P.BPANAM_0 as organization_name
            ,P.ACC_0 as acc_code
            ,case when P.BPRSAC_0='SC' then 'AP' else 'AR' end as acc_mode
            ,case when P.PAM_0='BNK' then 'BANK'
                  when P.PAM_0='CSH' then 'CASH'
                  else P.PAM_0 end as pay_mode 
            ,P.DES_0 as narration
            ,P.ACCDAT_0 as transaction_date 
            ,G.DUDDAT_0 as due_date
            ,P.CREDATTIM_0 as created_at
            ,P.UPDDATTIM_0 as updated_at
            ,case when P.PAYTYP_0 in('TDSC','TDS') and P.BPRSAC_0='AR' then 'CTDS' 
                  when P.PAYTYP_0 in('TDSC','TDS') and P.BPRSAC_0='SC' then 'VTDS'
                  else P.PAYTYP_0 end as payment_code
            ,G.AMTCUR_0 as account_util_amt_curr
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,P.AMTCUR_0 as amount 
            ,P.AMTCUR_0 * GC.RATMLT_0 as led_amount
            ,P.BANPAYTPY_0 as bank_pay_amount
            ,case G.sign_flag when 0 then 1 else G.sign_flag end as sign_flag
            ,GC.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,GC.RATMLT_0 as exchange_rate
            ,G.TYP_0 as account_type
            ,G.UPDDATTIM_0 as utilized_updated_at
            ,case when P.BPRSAC_0='AR' then 
            (select XX1P4PANNO_0 from COGO2.BPCUSTOMER where BPCNUM_0=P.BPR_0)
            else (select XX1P4PANNO_0 from COGO2.BPSUPPLIER where BPSNUM_0=P.BPR_0) end as pan_number
            ,P.BAN_0 as bank_short_code
            from COGO2.PAYMENTH P INNER JOIN COGO2.GACCENTRY GC on (P.NUM_0 = GC.REF_0 and GC.FCY_0=P.FCY_0)
            INNER JOIN       
            (
             select NUM_0,TYP_0,FCY_0,SAC_0,BPR_0,DUDDAT_0,PAM_0,SUM(AMTCUR_0) as AMTCUR_0,SIGN(SUM(SNS_0*(AMTLOC_0-PAYLOC_0))) as sign_flag
             ,SUM(AMTLOC_0) as AMTLOC_0,SUM(PAYCUR_0) as PAYCUR_0,SUM(PAYLOC_0) as PAYLOC_0, MAX(UPDDATTIM_0) as UPDDATTIM_0 
             from  COGO2.GACCDUDATE G where SAC_0 in('AR','SC')
             group by NUM_0,TYP_0,FCY_0,SAC_0,BPR_0,DUDDAT_0,PAM_0
            ) G            
            on (GC.NUM_0 = G.NUM_0 and  G.SAC_0 = P.BPRSAC_0 and G.BPR_0 = P.BPR_0 and G.BPR_0<>'' and G.FCY_0=P.FCY_0)
            where P.BPRSAC_0 in ('AR','SC') 
            and G.BPR_0 not in ${MigrationConstants.administrativeExpense}
            """
        sqlQuery += if (updatedAt == null) {
            """  and P.ACCDAT_0 BETWEEN '$startDate' and '$endDate' order by P.ACCDAT_0 ASC """
        } else {
            """ and cast(G.UPDDATTIM_0 as date) ='$updatedAt'"""
        }
        val paymentRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(paymentRecords, PaymentRecordManager::class.java)
        return payments.recordSets!![0]
    }

    override suspend fun migratePaymentByPaymentNum(paymentNums: String): ArrayList<PaymentRecord> {
        val sqlQuery = """
             SELECT  P.FCY_0 as entity_code 
            ,P.BPR_0 as sage_organization_id 
            ,P.NUM_0 as sage_ref_number
            ,P.BPANAM_0 as organization_name
            ,P.ACC_0 as acc_code
            ,case when P.BPRSAC_0='SC' then 'AP' else 'AR' end as acc_mode
            ,case when P.PAM_0='BNK' then 'BANK'
                  when P.PAM_0='CSH' then 'CASH'
                  else P.PAM_0 end as pay_mode 
            ,P.DES_0 as narration
            ,P.ACCDAT_0 as transaction_date 
            ,G.DUDDAT_0 as due_date
            ,P.CREDATTIM_0 as created_at
            ,P.UPDDATTIM_0 as updated_at
            ,case when P.PAYTYP_0 in('TDSC','TDS') and P.BPRSAC_0='AR' then 'CTDS' 
                  when P.PAYTYP_0 in('TDSC','TDS') and P.BPRSAC_0='SC' then 'VTDS'
                  else P.PAYTYP_0 end as payment_code
            ,G.AMTCUR_0 as account_util_amt_curr
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,P.AMTCUR_0 as amount 
            ,P.AMTCUR_0 * GC.RATMLT_0 as led_amount
            ,P.BANPAYTPY_0 as bank_pay_amount
            ,case G.sign_flag when 0 then 1 else G.sign_flag end as sign_flag
            ,GC.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,GC.RATMLT_0 as exchange_rate
            ,G.TYP_0 as account_type
            ,case when P.BPRSAC_0='AR' then 
            (select XX1P4PANNO_0 from COGO2.BPCUSTOMER where BPCNUM_0=P.BPR_0)
            else (select XX1P4PANNO_0 from COGO2.BPSUPPLIER where BPSNUM_0=P.BPR_0) end as pan_number
            ,P.BAN_0 as bank_short_code
            from COGO2.PAYMENTH P INNER JOIN COGO2.GACCENTRY GC on (P.NUM_0 = GC.REF_0 and GC.FCY_0=P.FCY_0)
            INNER JOIN       
            (
             select NUM_0,TYP_0,FCY_0,SAC_0,BPR_0,DUDDAT_0,PAM_0,SUM(AMTCUR_0) as AMTCUR_0,SIGN(SUM(SNS_0*(AMTLOC_0-PAYLOC_0))) as sign_flag
             ,SUM(AMTLOC_0) as AMTLOC_0,SUM(PAYCUR_0) as PAYCUR_0,SUM(PAYLOC_0) as PAYLOC_0 from  COGO2.GACCDUDATE G where SAC_0 in('AR','SC')
             group by NUM_0,TYP_0,FCY_0,SAC_0,BPR_0,DUDDAT_0,PAM_0
            ) G            
            on (GC.NUM_0 = G.NUM_0 and  G.SAC_0 = P.BPRSAC_0 and G.BPR_0 = P.BPR_0 and G.BPR_0<>'' and G.FCY_0=P.FCY_0)
            where P.BPRSAC_0 in ('AR','SC') 
            and G.BPR_0 not in ${MigrationConstants.administrativeExpense}
            and GC.NUM_0 in ($paymentNums) order by P.ACCDAT_0 ASC;
        """

        val paymentRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(paymentRecords, PaymentRecordManager::class.java)
        return payments.recordSets!![0]
    }

    override suspend fun getSettlementDataFromSage(
        startDate: String,
        endDate: String,
        source: String?,
        destination: String?
    ): ArrayList<SettlementRecord> {
        var sqlQuery = """
            SELECT  P.FCYLIN_0 as entity_code
            ,P.BPRLIN_0 as sage_organization_id
            ,GC.NUM_0 as sage_ref_number
            ,P.VCRTYP_0 as destination_type
            ,P.VCRNUM_0 as invoice_id
            ,GC.CUR_0 as currency
            ,GC.CURLED_0 as ledger_currency
            ,P.DENCOD_0 as source_type
            ,case when P.BPRSACINV_0='SC' then 'AP' else 'AR' end as acc_mode
            ,P.ACC_0 as acc_code
            ,P.PAYCURLIN_0 as currency_amount
            ,P.PAYLOCLIN_0 as ledger_amount
            ,P.CREDATTIM_0 as created_at
            ,P.UPDDATTIM_0 as updated_at
            from COGO2.PAYMENTD P INNER JOIN COGO2.GACCENTRY GC on (P.NUM_0 = GC.REF_0 and GC.FCY_0=P.FCYLIN_0)
            where P.BPRINV_0 != ' ' and P.VCRTYP_0 != ' ' and P.VCRNUM_0 != ' ' and GC.ACCDAT_0 BETWEEN '$startDate' and '$endDate'
        """.trimIndent()
        if (source != null && destination != null) {
            sqlQuery += """and GC.NUM_0 = '$source' and P.VCRNUM_0 = '$destination'"""
        }
        val paymentRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(paymentRecords, SettlementRecordManager::class.java)
        return payments.recordSets!![0]
    }

    override suspend fun getInvoicesPayLocDetails(
        startDate: String?,
        endDate: String?,
        updatedAt: String?,
        invoiceNumbers: String?
    ): ArrayList<InvoiceDetails> {
        var sqlQuery = """
                select case when si.GTE_0 in('ZSINV','ZSDN','ZDN') then 'INVOICE' else 'CREDIT_NOTE' end  as invoiceType
                ,si.AMTATIL_0 as ledger_total
                ,si.NUM_0 as invoice_number
                ,si.FCY_0 as entity_code_num
                ,si.BPR_0 as sage_organization_id
                ,acc.PAYCUR_0 as currency_amount_paid
                ,acc.PAYLOC_0 as ledger_amount_paid
                ,si.CREDATTIM_0 as created_at
                ,si.UPDDATTIM_0 as updated_at
                ,acc.UPDDATTIM_0  as utilization_updated_at
                ,si.BPRSAC_0 as acc_mode
                from COGO2.SINVOICE si with (NOLOCK)
                INNER JOIN COGO2.GACCDUDATE acc with (NOLOCK) on (si.NUM_0=acc.NUM_0 and  si.BPR_0=acc.BPR_0  and acc.TYP_0 =si.GTE_0 and acc.ACCNUM_0=si.ACCNUM_0)
        """.trimIndent()
        sqlQuery += if (invoiceNumbers != null) {
            """ where si.NUM_0 in $invoiceNumbers"""
        } else if (updatedAt == null) {
            """ where si.ACCDAT_0 between '$startDate' and '$endDate' order by si.ACCDAT_0 desc """
        } else {
            """ where cast(acc.UPDDATTIM_0 as date) = '$updatedAt' """
        }
        val result = Client.sqlQuery(sqlQuery)
        val invoiceDetails = ObjectMapper().readValue(result, InvoiceDetailRecordManager::class.java)
        return invoiceDetails.recordSets!![0]
    }

    override suspend fun getBillPayLocDetails(startDate: String?, endDate: String?, updatedAt: String?): ArrayList<InvoiceDetails> {
        var sqlQuery = """
            select  case when si.GTE_0 in('SPINV') then 'BILL' else 'CREDIT_NOTE' end  as invoiceType
                            ,si.AMTATIL_0 as ledgerTotal
                            ,si.NUM_0 as invoiceNumber
                            ,si.FCY_0 as entityCodeNum
                            ,si.BPR_0 as sageOrganizationId
                            ,acc.PAYCUR_0 as currencyAmountPaid
                            ,acc.PAYLOC_0 as ledgerAmountPaid
                            ,si.CREDATTIM_0 as createdAt
                            ,si.UPDDATTIM_0 as updatedAt
                            ,acc.UPDDATTIM_0  as utilization_updated_at
                            ,case when si.BPRSAC_0 = 'SC' then 'AP' else si.BPRSAC_0 end as acc_mdoe
                            from COGO2.PINVOICE si with (NOLOCK)
                            INNER JOIN COGO2.GACCDUDATE acc with (NOLOCK) on (si.NUM_0=acc.NUM_0 and  si.BPR_0=acc.BPR_0  and acc.TYP_0 =si.GTE_0 and acc.ACCNUM_0=si.ACCNUM_0)
        """.trimIndent()
        sqlQuery += if (updatedAt == null) {
            """ where si.ACCDAT_0 between '$startDate' and '$endDate' order by si.ACCDAT_0 desc """
        } else {
            """ where cast(acc.UPDDATTIM_0 as date) = '$updatedAt' """
        }
        val result = Client.sqlQuery(sqlQuery)
        val invoiceDetails = ObjectMapper().readValue(result, InvoiceDetailRecordManager::class.java)
        return invoiceDetails.recordSets!![0]
    }

    override suspend fun getJVDetails(startDate: String?, endDate: String?, jvNum: String?, sageJvId: String?): List<JVParentDetails> {
        var sqlQuery = """
            select NUM_0 as jv_num, TYP_0 as jv_type,'POSTED' as jv_status,CREDAT_0 as created_at, UPDDAT_0 as updated_at, VALDAT_0 as validity_date, CUR_0 as currency, CURLED_0 as ledger_currency
            ,RATMLT_0 as exchange_rate, 0 as amount, DESVCR_0 as description,JOU_0 as jv_code_num from COGO2.GACCENTRY where TYP_0 in ('BANK','CONTR','INTER','MISC','MTC','MTCCV','OPDIV')
        """.trimIndent()
        sqlQuery += if (sageJvId != null) {
            """ and ROWID = $sageJvId"""
        } else if (startDate != null && endDate != null) {
            """ and CREDAT_0 between '$startDate' and '$endDate'"""
        } else {
            """ and NUM_0 in ($jvNum)"""
        }
        val result = Client.sqlQuery(sqlQuery)
        val parentDetails = ObjectMapper().readValue(result, JVParentRecordManger::class.java)
        return parentDetails.recordSets!![0]
    }

    suspend fun getJVLineItemWithNoBPR(jvNum: String, jvType: String): List<JVLineItemNoBPR> {
        val sqlQuery = """
            select FCYLIN_0 as entityCode
            ,GD.NUM_0 as jvNum
            , GD.TYP_0 as type
            ,G.VALDAT_0 as validityDate
            ,AMTCUR_0 as amount
            , AMTLED_0 as ledger_amount
            ,GD.CUR_0 as currency
            , GD.CURLED_0 as ledgerCurrency
            ,'POSTED' as status
            , G.RATMLT_0 as exchange_rate
            , GD.CREDATTIM_0 as created_at
            , GD.UPDDATTIM_0 as updated_at
            , GD.DES_0 as description
            , GD.ROWID as sage_unique_id
            , GD.SNS_0 as sign_flag
            , GD.ACC_0 as gl_code 
            ,GD.BPR_0 as sage_organization_id
            ,case when SAC_0 = 'SC' then 'AP' else SAC_0 end as acc_mode
            from COGO2.GACCENTRY G inner join COGO2.GACCENTRYD GD on (G.NUM_0 = GD.NUM_0 and G.TYP_0 = GD.TYP_0)  
            where BPR_0 = '' and SAC_0 = ''
            and GD.TYP_0 in ('$jvType')
            and GD.NUM_0 = '$jvNum'
            and GD.BPR_0 not in ${MigrationConstants.administrativeExpense}
        """.trimIndent()
        val result = Client.sqlQuery(sqlQuery)
        val jvLineItemNoBPR = ObjectMapper().readValue(result, JVRecordsWithoutBprManager::class.java)
        return jvLineItemNoBPR.recordSets!![0]
    }

    override suspend fun getJournalVoucherFromSageCorrected(
        startDate: String?,
        endDate: String?,
        jvNums: String?,
        jvType: String?
    ): ArrayList<JournalVoucherRecord> {
        var sqlQuery = """
         SELECT G.FCY_0 as entity_code 
            ,G.BPR_0 as sage_organization_id 
            ,G.NUM_0 as payment_num
            ,case when G.SAC_0='AR' then 
            (select BPCNAM_0 from COGO2.BPCUSTOMER where BPCNUM_0=G.BPR_0)
            else (select BPSNAM_0 from COGO2.BPSUPPLIER where BPSNUM_0=G.BPR_0) end as organization_name
            ,GD.ACC_0 as acc_code
            ,case when G.SAC_0='SC' then 'AP' else G.SAC_0 end as acc_mode
            ,case when G.PAM_0='BNK' then 'BANK'
                  when G.PAM_0='CSH' then 'CASH'
                  else G.PAM_0 end as pay_mode 
            ,GD.DES_0 as narration
            ,GC.ACCDAT_0 as transaction_date 
            ,G.DUDDAT_0 as due_date
            ,GD.CREDATTIM_0 as created_at
            ,GD.UPDDATTIM_0 as updated_at
            ,GC.RATMLT_0 as exchange_rate
            ,case when G.SAC_0='AR' then 'REC' else 'PAY' end  as payment_code
            ,G.AMTCUR_0 as account_util_amt_curr    
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,case G.sign_flag when 0 then 1 else G.sign_flag end as sign_flag
            ,G.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,G.TYP_0 as account_type
            ,GD.ROWID as sage_unique_id
            from  COGO2.GACCENTRY GC 
            INNER JOIN           
            (
            select TYP_0,NUM_0,FCY_0,CUR_0,SAC_0,BPR_0,DUDDAT_0,PAM_0, AMTCUR_0 as AMTCUR_0, AMTLOC_0 as AMTLOC_0, PAYCUR_0 as PAYCUR_0, PAYLOC_0 as PAYLOC_0
            ,SNS_0 as sign_flag, LIG_0
            from  COGO2.GACCDUDATE where SAC_0 in('AR','SC','CSD','PDA','EMD','SUSS','SUSA') and TYP_0 in ('$jvType')
            ) G 
            on (GC.NUM_0 = G.NUM_0 and GC.FCY_0=G.FCY_0)
            INNER JOIN COGO2.GACCENTRYD GD on (GD.NUM_0 = GC.NUM_0 and GD.TYP_0 = G.TYP_0 and GD.SAC_0 = G.SAC_0 and GD.AMTCUR_0 = G.AMTCUR_0 and GD.BPR_0 = G.BPR_0)
            where G.SAC_0 in('AR','SC','CSD','PDA','EMD','SUSS','SUSA') and G.TYP_0 in ('$jvType') and G.LIG_0 = GD.LIN_0
            and G.BPR_0 not in ${MigrationConstants.administrativeExpense}
            """
        if (startDate == null && endDate == null) {
            sqlQuery += """and G.NUM_0 in ($jvNums) order by GC.ACCDAT_0 ASC"""
        } else {
            sqlQuery += """and GC.ACCDAT_0 BETWEEN '$startDate' and '$endDate' order by GC.ACCDAT_0 ASC"""
        }
        val journalRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(journalRecords, JournalVoucherRecordManager::class.java)
        return payments.recordSets!![0]
    }

    override suspend fun getPaymentsForScheduler(startDate: String, endDate: String): ArrayList<PaymentRecord> {
        var sqlQuery =
            """
            SELECT  P.FCY_0 as entity_code 
            ,P.BPR_0 as sage_organization_id 
            ,P.NUM_0 as sage_ref_number
            ,P.BPANAM_0 as organization_name
            ,P.ACC_0 as acc_code
            ,case when P.BPRSAC_0='SC' then 'AP' else 'AR' end as acc_mode
            ,case when P.PAM_0='BNK' then 'BANK'
                  when P.PAM_0='CSH' then 'CASH'
                  else P.PAM_0 end as pay_mode 
            ,P.DES_0 as narration
            ,P.ACCDAT_0 as transaction_date 
            ,G.DUDDAT_0 as due_date
            ,P.CREDATTIM_0 as created_at
            ,P.UPDDATTIM_0 as updated_at
            ,case when P.PAYTYP_0 in('TDSC','TDS') and P.BPRSAC_0='AR' then 'CTDS' 
                  when P.PAYTYP_0 in('TDSC','TDS') and P.BPRSAC_0='SC' then 'VTDS'
                  else P.PAYTYP_0 end as payment_code
            ,G.AMTCUR_0 as account_util_amt_curr
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,P.AMTCUR_0 as amount 
            ,P.AMTCUR_0 * GC.RATMLT_0 as led_amount
            ,P.BANPAYTPY_0 as bank_pay_amount
            ,case G.sign_flag when 0 then 1 else G.sign_flag end as sign_flag
            ,GC.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,GC.RATMLT_0 as exchange_rate
            ,G.TYP_0 as account_type
            ,G.UPDDATTIM_0 as utilized_updated_at
            ,case when P.BPRSAC_0='AR' then 
            (select XX1P4PANNO_0 from COGO2.BPCUSTOMER where BPCNUM_0=P.BPR_0)
            else (select XX1P4PANNO_0 from COGO2.BPSUPPLIER where BPSNUM_0=P.BPR_0) end as pan_number
            ,P.BAN_0 as bank_short_code
            from COGO2.PAYMENTH P INNER JOIN COGO2.GACCENTRY GC on (P.NUM_0 = GC.REF_0 and GC.FCY_0=P.FCY_0)
            INNER JOIN       
            (
             select NUM_0,TYP_0,FCY_0,SAC_0,BPR_0,DUDDAT_0,PAM_0,SUM(AMTCUR_0) as AMTCUR_0,SIGN(SUM(SNS_0*(AMTLOC_0-PAYLOC_0))) as sign_flag
             ,SUM(AMTLOC_0) as AMTLOC_0,SUM(PAYCUR_0) as PAYCUR_0,SUM(PAYLOC_0) as PAYLOC_0, MAX(UPDDATTIM_0) as UPDDATTIM_0 
             from  COGO2.GACCDUDATE G where SAC_0 in('AR','SC')
             group by NUM_0,TYP_0,FCY_0,SAC_0,BPR_0,DUDDAT_0,PAM_0
            ) G            
            on (GC.NUM_0 = G.NUM_0 and  G.SAC_0 = P.BPRSAC_0 and G.BPR_0 = P.BPR_0 and G.BPR_0<>'' and G.FCY_0=P.FCY_0)
            where P.BPRSAC_0 in ('AR','SC') 
            and G.BPR_0 not in ${MigrationConstants.administrativeExpense}
            and P.CREDATTIM_0 BETWEEN '$startDate' and '$endDate' order by P.CREDATTIM_0 ASC
            """
        val paymentRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(paymentRecords, PaymentRecordManager::class.java)
        return payments.recordSets!![0]
    }

    override suspend fun getNewPeriodRecord(startDate: String, endDate: String, bpr: String?, accMode: String): List<NewPeriodRecord> {
        var sqlQuery = """
            select 
            TYP_0 as acc_type
            ,NUM_0 as document_value
            ,FCY_0 as cogo_entity
            ,CUR_0 as currency
            ,SAC_0 as accMode
            ,BPR_0 as sageOrganizationId
            ,DUDDAT_0 as transactionDate
            ,AMTCUR_0 as amountCurr
            ,AMTLOC_0 as amountLoc
            ,PAYCUR_0 as payCurr
            ,PAYLOC_0 as payLoc
            ,CREDATTIM_0 as createdAt
            ,UPDDATTIM_0 as updatedAt
            ,SNS_0 as sign_flag
            from COGO2.GACCDUDATE where TYP_0 = 'NEWPR' and SAC_0 in ('$accMode') and cast(DUDDAT_0 as date) between'20220401' and '20220401'
        """.trimIndent()
        if (bpr != null) {
            sqlQuery += """ and BPR_0 = '$bpr'"""
        }
        val result = Client.sqlQuery(sqlQuery)
        val newPeriodRecords = ObjectMapper().readValue(result, NewPeriodRecordManager::class.java)
        return newPeriodRecords.recordSets!![0]
    }

    override suspend fun getJVDetailsForScheduler(startDate: String?, endDate: String?, jvNum: String?): List<JVRecordsScheduler> {
        var sqlQuery = """
            SELECT G.FCY_0 as entity_code 
            ,G.BPR_0 as sage_organization_id 
            ,G.NUM_0 as payment_num
            ,GD.ACC_0 as acc_code
            ,case when G.SAC_0='SC' then 'AP' else G.SAC_0 end as acc_mode
            ,case when G.SAC_0='AR' then 'REC' else 'PAY' end  as payment_code
            ,G.AMTCUR_0 as account_util_amt_curr    
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,case G.sign_flag when 0 then 1 else G.sign_flag end as sign_flag
            ,G.TYP_0 as account_type
            ,GD.ROWID as sage_unique_id
            from  COGO2.GACCENTRY GC 
            INNER JOIN
            (
            select TYP_0,NUM_0,FCY_0,CUR_0,SAC_0,BPR_0,DUDDAT_0,PAM_0, AMTCUR_0 as AMTCUR_0, AMTLOC_0 as AMTLOC_0, PAYCUR_0 as PAYCUR_0, PAYLOC_0 as PAYLOC_0
            ,SNS_0 as sign_flag, LIG_0, UPDDATTIM_0
            from  COGO2.GACCDUDATE where SAC_0 in('AR','SC','CSD','PDA','EMD','SUSS','SUSA') and TYP_0 in ('BANK','CONTR','INTER','MISC','MTC','MTCCV','OPDIV')
            ) G 
            on (GC.NUM_0 = G.NUM_0 and GC.FCY_0=G.FCY_0)
            INNER JOIN COGO2.GACCENTRYD GD on (GD.NUM_0 = GC.NUM_0 and GD.TYP_0 = G.TYP_0 and GD.SAC_0 = G.SAC_0 and GD.AMTCUR_0 = G.AMTCUR_0 and GD.BPR_0 = G.BPR_0)
            where G.SAC_0 in('AR','SC','CSD','PDA','EMD','SUSS','SUSA') and G.TYP_0 in ('BANK','CONTR','INTER','MISC','MTC','MTCCV','OPDIV') and G.LIG_0 = GD.LIN_0
        """.trimIndent()
        sqlQuery += if (jvNum.isNullOrEmpty()) {
            """ and G.UPDDATTIM_0 between '$startDate' and '$endDate'"""
        } else {
            """ and G.NUM_0 in ($jvNum)"""
        }
        val result = Client.sqlQuery(sqlQuery)
        val jvRecords = ObjectMapper().readValue(result, JVRecordsForSchedulerManager::class.java)
        return jvRecords.recordSets!![0]
    }

    override suspend fun getGLCode(): List<GlCodeMaster> {
        val sqlQuery = """
            SELECT 
                ACC_0 AS account_code,
                DES_0 AS description,
                COA_0 AS led_account, 
                CASE 
                    WHEN ACCSHO_0 ='SC'
                    THEN 'AP'
                ELSE
                    ACCSHO_0 
                END AS account_type,
                CLSCOD_0 AS class_code,
                'c4f72139-e4b9-4cea-b590-32cea179f441' AS created_by,
                'c4f72139-e4b9-4cea-b590-32cea179f441' AS updated_by,
                CREDAT_0 AS created_at, 
                UPDDAT_0 as updated_at
            FROM 
                COGO2.GACCOUNT
        """.trimIndent()
        val result = Client.sqlQuery(sqlQuery)
        val glCodeRecords = ObjectMapper().readValue(result, GlCodeRecordsManager::class.java)
        return glCodeRecords.recordSets!![0]
    }

    override suspend fun getPaymentPostSageInfo(
        paymentNumValue: String,
        entityCode: Long?,
        accMode: AccMode
    ): PaymentDetailsInfo? {
        val platformPaymentDetails = paymentRepository.getPaymentByPaymentNumValue(paymentNumValue, entityCode, accMode)

        if (platformPaymentDetails == null) {
            throw AresException(AresError.ERR_1539, "")
        }
        val paymentDetails = PlatformPostPaymentDetails(
            sagePaymentNum = platformPaymentDetails.sageRefNumber ?: " ",
            platformPaymentNum = platformPaymentDetails.paymentNumValue,
            bprNumber = platformPaymentDetails.sageOrganizationId,
            glCode = platformPaymentDetails.accCode,
            currency = platformPaymentDetails.currency!!,
            entityCode = platformPaymentDetails.entityCode!!.toLong(),
            amount = platformPaymentDetails.amount,
            status = platformPaymentDetails.paymentDocumentStatus.toString(),
            organizationName = platformPaymentDetails.organizationName
        )

        var accountMode = accMode.name
        if (accountMode == AccMode.AP.name) {
            accountMode = "SC"
        }

        val sagePaymentDetails = when (platformPaymentDetails.migrated) {
            true -> getMigratedPaymentSageInfo(platformPaymentDetails.sageRefNumber!!, entityCode, accountMode)
            else -> getPaymentSageInfo(paymentNumValue, entityCode, accountMode)
        }

        return PaymentDetailsInfo(
            sagePaymentInfo = sagePaymentDetails,
            platformPaymentInfo = paymentDetails
        )
    }

    private fun getPaymentSageInfo(paymentNumValue: String, entityCode: Long?, accMode: String): SagePostPaymentDetails? {
        val sqlQuery = """
            select NUM_0 as sage_payment_num, UMRNUM_0 as platform_payment_num, 
            case WHEN STA_0 = 9 THEN 'FINAL_POSTED'
                 WHEN STA_0 = 1 THEN 'POSTED' end as sage_status, 
                 BPR_0 as bpr_number, 
                 ACC_0 as gl_code, 
                 CUR_0 as currency, 
                 FCY_0 as entity_code, 
                 AMTCUR_0 as amount,
                 BPANAM_0 as organization_name
            from $sageDatabase.PAYMENTH where UMRNUM_0 in ('$paymentNumValue') and FCY_0 = $entityCode and BPRSAC_0 = '$accMode'
        """.trimIndent()
        val result = Client.sqlQuery(sqlQuery)
        val paymentRecords = ObjectMapper().readValue(result, PostPaymentInfo::class.java)

        if (paymentRecords.recordSets!![0].isNullOrEmpty()) {
            return null
        }
        return paymentRecords.recordSets!![0].first()
    }

    private fun getMigratedPaymentSageInfo(paymentNumValue: String, entityCode: Long?, accMode: String): SagePostPaymentDetails? {
        val sqlQuery = """
            SELECT P.NUM_0 AS sage_payment_num, P.UMRNUM_0 as platform_payment_num, 
            CASE WHEN P.STA_0 = 9 THEN 'FINAL_POSTED'
                 WHEN P.STA_0 = 1 THEN 'POSTED' end as sage_status, 
                 P.BPR_0 as bpr_number, 
                 P.ACC_0 as gl_code, 
                 P.CUR_0 as currency, 
                 P.FCY_0 as entity_code, 
                 P.AMTCUR_0 as amount,
                 P.BPANAM_0 as organization_name
            from $sageDatabase.PAYMENTH P
            JOIN $sageDatabase.GACCENTRY GA on P.NUM_0 = GA.REF_0
            where GA.NUM_0 in ('$paymentNumValue') and P.FCY_0 = $entityCode and P.BPRSAC_0 = '$accMode'
        """.trimIndent()
        val result = Client.sqlQuery(sqlQuery)
        val paymentRecords = ObjectMapper().readValue(result, PostPaymentInfo::class.java)

        if (paymentRecords.recordSets!![0].isNullOrEmpty()) {
            return null
        }

        return paymentRecords.recordSets!![0].first()
    }

    override suspend fun getSagePaymentNum(sageRefNumber: List<String>): ArrayList<SagePaymentNumMigrationResponse> {
        val sqlQuery = """
            select NUM_0 as sage_ref_num, REF_0 as sage_payment_num from $sageDatabase.GACCENTRY where NUM_0 in ('${sageRefNumber.joinToString("','")}')
        """.trimIndent()
        val result = Client.sqlQuery(sqlQuery)
        val paymentRecords = ObjectMapper().readValue(result, PaymentNumInfo::class.java)

        return paymentRecords.recordSets!![0]
    }

    override suspend fun getMTCJVDetails(startDate: String?, endDate: String?): List<JVParentDetails> {
        var sqlQuery = """
            select NUM_0 as jv_num, TYP_0 as jv_type,'POSTED' as jv_status,CREDAT_0 as created_at, UPDDAT_0 as updated_at, VALDAT_0 as validity_date, CUR_0 as currency, CURLED_0 as ledger_currency
            ,RATMLT_0 as exchange_rate, 0 as amount, DESVCR_0 as description,JOU_0 as jv_code_num from $sageDatabase.GACCENTRY where TYP_0 in ('MTC','MTCCV')
        """.trimIndent()
        sqlQuery += if (startDate != null && endDate != null) {
            """ and CREDAT_0 between '$startDate' and '$endDate'"""
        } else {
            """ and CREDAT_0 between now() - INTERVAL '1 day' and now()"""
        }
        val result = Client.sqlQuery(sqlQuery)
        val parentDetails = ObjectMapper().readValue(result, JVParentRecordManger::class.java)
        return parentDetails.recordSets!![0]
    }

    fun getTDSJVLineItemWithNoBPR(jvNum: String, jvType: String): List<JVLineItemNoBPR> {
        val sqlQuery = """
            select FCYLIN_0 as entityCode
            ,GD.NUM_0 as jvNum
            , 'VTDS' as type
            ,G.VALDAT_0 as validityDate
            ,AMTCUR_0 as amount
            , AMTLED_0 as ledger_amount
            ,GD.CUR_0 as currency
            , GD.CURLED_0 as ledgerCurrency
            ,'POSTED' as status
            , G.RATMLT_0 as exchange_rate
            , GD.CREDATTIM_0 as created_at
            , GD.UPDDATTIM_0 as updated_at
            , GD.DES_0 as description
            , GD.ROWID as sage_unique_id
            , GD.SNS_0 as sign_flag
            , GD.ACC_0 as gl_code 
            ,GD.BPR_0 as sage_organization_id
            ,case when SAC_0 = 'SC' then 'AP' else SAC_0 end as acc_mode
            from COGO2.GACCENTRY G inner join COGO2.GACCENTRYD GD on (G.NUM_0 = GD.NUM_0 and G.TYP_0 = GD.TYP_0)  
            where BPR_0 = '' and SAC_0 = '' and GD.DES_0 in ('TDS', 'tds')
            and GD.TYP_0 in ('$jvType')
            and GD.NUM_0 = '$jvNum'
            and GD.BPR_0 not in ${MigrationConstants.administrativeExpense}
        """.trimIndent()
        val result = Client.sqlQuery(sqlQuery)
        val jvLineItemNoBPR = ObjectMapper().readValue(result, JVRecordsWithoutBprManager::class.java)
        return jvLineItemNoBPR.recordSets!![0]
    }

    override suspend fun getTDSJournalVoucherFromSageCorrected(
        startDate: String?,
        endDate: String?,
        jvNums: String?,
        jvType: String?
    ): ArrayList<JournalVoucherRecord> {
        var sqlQuery = """
         SELECT G.FCY_0 as entity_code 
            ,G.BPR_0 as sage_organization_id 
            ,G.NUM_0 as payment_num
            ,case when G.SAC_0='AR' then 
            (select BPCNAM_0 from COGO2.BPCUSTOMER where BPCNUM_0=G.BPR_0)
            else (select BPSNAM_0 from COGO2.BPSUPPLIER where BPSNUM_0=G.BPR_0) end as organization_name
            ,GD.ACC_0 as acc_code
            ,case when G.SAC_0='SC' then 'AP' else G.SAC_0 end as acc_mode
            ,case when G.PAM_0='BNK' then 'BANK'
                  when G.PAM_0='CSH' then 'CASH'
                  else G.PAM_0 end as pay_mode 
            ,GD.DES_0 as narration
            ,GC.ACCDAT_0 as transaction_date 
            ,G.DUDDAT_0 as due_date
            ,GD.CREDATTIM_0 as created_at
            ,GD.UPDDATTIM_0 as updated_at
            ,GC.RATMLT_0 as exchange_rate
            ,case when G.SAC_0='AR' then 'REC' else 'PAY' end  as payment_code
            ,G.AMTCUR_0 as account_util_amt_curr    
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,case G.sign_flag when 0 then 1 else G.sign_flag end as sign_flag
            ,G.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,G.TYP_0 as account_type
            ,GD.ROWID as sage_unique_id
            from  COGO2.GACCENTRY GC 
            INNER JOIN           
            (
            select TYP_0,NUM_0,FCY_0,CUR_0,SAC_0,BPR_0,DUDDAT_0,PAM_0, AMTCUR_0 as AMTCUR_0, AMTLOC_0 as AMTLOC_0, PAYCUR_0 as PAYCUR_0, PAYLOC_0 as PAYLOC_0
            ,SNS_0 as sign_flag, LIG_0
            from  COGO2.GACCDUDATE where SAC_0 in('AR','SC','CSD','PDA','EMD','SUSS','SUSA') and TYP_0 in ('$jvType')
            ) G 
            on (GC.NUM_0 = G.NUM_0 and GC.FCY_0=G.FCY_0)
            INNER JOIN COGO2.GACCENTRYD GD on (GD.NUM_0 = GC.NUM_0 and GD.TYP_0 = G.TYP_0 and GD.SAC_0 = G.SAC_0 and GD.AMTCUR_0 = G.AMTCUR_0 and GD.BPR_0 = G.BPR_0)
            where G.SAC_0 in('AR','SC','CSD','PDA','EMD','SUSS','SUSA') and G.TYP_0 in ('$jvType') and G.LIG_0 = GD.LIN_0 and GD.DES_0 in ('TDS', 'tds')
            and G.BPR_0 not in ${MigrationConstants.administrativeExpense}
            """
        if (startDate == null && endDate == null) {
            sqlQuery += """and G.NUM_0 in ($jvNums) order by GC.ACCDAT_0 ASC"""
        } else {
            sqlQuery += """and GC.ACCDAT_0 BETWEEN '$startDate' and '$endDate' order by GC.ACCDAT_0 ASC"""
        }
        val journalRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(journalRecords, JournalVoucherRecordManager::class.java)
        return payments.recordSets!![0]
    }

    override suspend fun getTDSJVDetails(startDate: String?, endDate: String?, jvNum: String?, sageJvId: String?): List<JVParentDetails> {
        var sqlQuery = """
            select NUM_0 as jv_num, TYP_0 as jv_type,'POSTED' as jv_status,CREDAT_0 as created_at, UPDDAT_0 as updated_at, VALDAT_0 as validity_date, CUR_0 as currency, CURLED_0 as ledger_currency
            ,RATMLT_0 as exchange_rate, 0 as amount, DESVCR_0 as description,JOU_0 as jv_code_num from COGO2.GACCENTRY where TYP_0 in ('BANK','CONTR','INTER','MISC','MTC','MTCCV','OPDIV', 'SPINV')
        """.trimIndent()
        sqlQuery += if (sageJvId != null) {
            """ and ROWID = $sageJvId"""
        } else if (startDate != null && endDate != null) {
            """ and CREDAT_0 between '$startDate' and '$endDate'"""
        } else {
            """ and NUM_0 in ($jvNum)"""
        }
        val result = Client.sqlQuery(sqlQuery)
        val parentDetails = ObjectMapper().readValue(result, JVParentRecordManger::class.java)
        return parentDetails.recordSets!![0]
    }
}
