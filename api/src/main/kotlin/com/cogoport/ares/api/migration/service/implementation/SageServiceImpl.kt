package com.cogoport.ares.api.migration.service.implementation

import com.cogoport.ares.api.migration.model.JournalVoucherRecord
import com.cogoport.ares.api.migration.model.JournalVoucherRecordManager
import com.cogoport.ares.api.migration.model.PaymentRecord
import com.cogoport.ares.api.migration.model.PaymentRecordManager
import com.cogoport.ares.api.migration.service.interfaces.SageService
import com.cogoport.brahma.sage.Client
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@Singleton
class SageServiceImpl : SageService {

    @Value("\${sage.databaseName}")
    var sageSchema: String? = null

    override suspend fun getPaymentDataFromSage(startDate: String?, endDate: String?): ArrayList<PaymentRecord> {
        val sqlQuery = """
         SELECT  P.FCY_0 as entity_code 
            ,P.BPR_0 as sage_organization_id 
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
            ,GC.NUM_0 as payment_num
            ,G.AMTCUR_0 as account_util_amt_curr
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,P.AMTCUR_0 as amount 
            ,P.AMTCUR_0 * GC.RATMLT_0 as led_amount
            ,P.BANPAYTPY_0 as bank_pay_amount
            ,G.SNS_0 as sign_flag
            ,GC.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,GC.RATMLT_0 as exchange_rate
            ,G.TYP_0 as account_type
            ,case when P.BPRSAC_0='AR' then 
            (select XX1P4PANNO_0 from COGO2.BPCUSTOMER where BPCNUM_0=P.BPR_0)
            else (select XX1P4PANNO_0 from COGO2.BPSUPPLIER where BPSNUM_0=P.BPR_0) end as pan_number
            ,P.BAN_0 as bank_short_code
            from COGO2.PAYMENTH P INNER JOIN COGO2.GACCENTRY GC on P.NUM_0 = GC.REF_0
            INNER JOIN COGO2.GACCDUDATE G on GC.NUM_0 = G.NUM_0
            where P.BPRSAC_0 in('AR','SC') 
            and P.ACCDAT_0 BETWEEN '$startDate' and '$endDate' order by P.ACCDAT_0 ASC
             """
        //  -- and P.ACCDAT_0 BETWEEN '$startDate' and '$endDate' order by P.ACCDAT_0 ASC
        val paymentRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(paymentRecords, PaymentRecordManager::class.java)
        return payments.recordSets!![0]
    }

    override suspend fun getJournalVoucherFromSage(
        startDate: String?,
        endDate: String?
    ): ArrayList<JournalVoucherRecord> {
        val sqlQuery = """
         SELECT   G.FCY_0 as entity_code 
            ,G.BPR_0 as sage_organization_id 
            ,case when G.SAC_0='AR' then 
            (select BPCNAM_0 from COGO2.BPCUSTOMER where BPCNUM_0=G.BPR_0)
            else (select BPSNAM_0 from COGO2.BPSUPPLIER where BPSNUM_0=G.BPR_0) end as organization_name
            ,case when G.SAC_0='AR' then 223000 else 321000 end as acc_code
            ,case when G.SAC_0='SC' then 'AP' else 'AR' end as acc_mode
            ,case when G.PAM_0='BNK' then 'BANK'
                  when G.PAM_0='CSH' then 'CASH'
                  else G.PAM_0 end as pay_mode 
            ,GC.DESVCR_0 as narration
            ,GC.ACCDAT_0 as transaction_date 
            ,G.DUDDAT_0 as due_date
            ,GC.CREDATTIM_0 as created_at
            ,GC.UPDDATTIM_0 as updated_at
            ,case when G.SAC_0='AR' then 'REC' else 'PAY' end  as payment_code
            ,G.NUM_0 as payment_num
            ,G.AMTCUR_0 as account_util_amt_curr
            ,G.AMTLOC_0 as account_util_amt_led
            ,G.PAYCUR_0 as account_util_pay_curr
            ,G.PAYLOC_0 as account_util_pay_led
            ,G.SNS_0 as sign_flag
            ,G.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,G.TYP_0 as account_type
            from  COGO2.GACCENTRY GC 
            INNER JOIN COGO2.GACCDUDATE G on GC.NUM_0 = G.NUM_0
            where G.SAC_0 in('AR','SC') and G.TYP_0 in('BANK','CONTR','INTER','MTC','MTCCV')
            and GC.ACCDAT_0 BETWEEN '$startDate' and '$endDate' order by GC.ACCDAT_0 ASC 
             """

        val journalRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(journalRecords, JournalVoucherRecordManager::class.java)
        return payments.recordSets!![0]
    }
}
