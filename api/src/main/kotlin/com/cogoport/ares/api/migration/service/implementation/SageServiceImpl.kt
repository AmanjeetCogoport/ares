package com.cogoport.ares.api.migration.service.implementation

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
            SELECT P.FCY_0 as entity_code 
            ,P.BPR_0 as sage_organization_id 
            ,P.BPANAM_0 as organization_name
            ,P.ACC_0 as acc_code,P.BPRSAC_0 as acc_mode
            ,P.AMTCUR_0 as amount 
            ,P.BANPAYTPY_0 as led_amount
            ,P.PAM_0 as pay_mode 
            ,P.DES_0 as narration
            ,P.ACCDAT_0 as transaction_date 
            ,P.CREDATTIM_0 as created_at
            ,P.UPDDATTIM_0 as updated_at
            ,P.PAYTYP_0 as payment_code
            ,GC.NUM_0 as payment_num
            ,GC.NUM_0 as payment_num_value
            ,G.SNS_0 as sign_flag
            ,GC.CUR_0 as currency
            ,GC.CURLED_0 as led_currency
            ,GC.RATMLT_0 as exchange_rate
            from $sageSchema.PAYMENTH P  LEFT JOIN $sageSchema.GACCENTRY GC on P.NUM_0 = GC.REF_0
            LEFT JOIN $sageSchema.GACCDUDATE G on GC.NUM_0 = G.NUM_0
            where P.ACCDAT_0 BETWEEN $startDate and $endDate order by P.ACCDAT_0 ASC """

        val paymentRecords = Client.sqlQuery(sqlQuery)
        val payments = ObjectMapper().readValue(paymentRecords, PaymentRecordManager::class.java)
        return payments.recordSets!![0]
    }
}
