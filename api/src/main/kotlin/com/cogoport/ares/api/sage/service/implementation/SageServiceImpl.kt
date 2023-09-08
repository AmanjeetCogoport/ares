package com.cogoport.ares.api.sage.service.implementation

import com.cogoport.ares.api.sage.service.interfaces.SageService
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.PostPaymentInfos
import com.cogoport.ares.model.payment.SagePaymentDetails
import com.cogoport.brahma.sage.Client
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micronaut.context.annotation.Value

class SageServiceImpl : SageService {
    // check if a document exist on sage

    @Value("\${sage.databaseName}")
    var sageDatabase: String? = null

    override suspend fun checkIfDocumentExistInSage(documentValue: String, sageBPRNumber: String, organizationSerialId: Long?, documentType: AccountType, registrationNumber: String?): String? {
        // Check if SINV, SCN exists and it is posted in sage
        if (arrayListOf(AccountType.SINV, AccountType.SCN, AccountType.SDN).contains(documentType)) {
            return isInvoiceDataPostedFromSage("NUM_0", "$sageDatabase.SINVOICE", documentValue)
        }
        // Check if PINV, PCN, PDN, EXP exists and it is posted in sage
        if (arrayListOf(AccountType.PINV, AccountType.PCN, AccountType.PDN, AccountType.EXP).contains(documentType)) {
            return isBillDataPostedFromSage(documentValue, organizationSerialId, sageBPRNumber, registrationNumber)
        }

        // Check if REC, PAY exists and it is posted in sage
        if (arrayListOf(AccountType.REC, AccountType.PAY, AccountType.CTDS).contains(documentType)) {
            return isPaymentPostedFromSage(documentValue)
        }

        return isJvPostedOnSage(documentValue)
    }

    private fun isBillDataPostedFromSage(billNumber: String?, organizationSerialId: Long?, sageOrganizationId: String?, registrationNumber: String?): String? {
        val query = """
            SELECT  * FROM $sageDatabase.PINVOICE P 
            	            INNER JOIN $sageDatabase.BPSUPPLIER BP ON (P.BPR_0 = BP.BPSNUM_0) 
            	            WHERE (P.BPRVCR_0 = '$billNumber'  
            	            OR P.BPRVCR_0 = '$billNumber&$organizationSerialId')
                            AND P.STA_0 = 3
            	            AND BP.XX1P4PANNO_0 = '$registrationNumber'
        """.trimIndent()
        val resultFromQuery = Client.sqlQuery(query)
        logger().info("billData: $resultFromQuery")
        val records = ObjectMapper().readValue<MutableMap<String, Any?>>(resultFromQuery).get("recordset") as ArrayList<*>
        if (records.size != 0) {
            val recordMap = records.toArray()[0] as HashMap<String, String>
            return recordMap["NUM_0"]
        }
        return null
    }

    private fun isInvoiceDataPostedFromSage(key: String, sageDatabase: String?, invoiceNumber: String?): String? {
        val query = "Select $key from $sageDatabase where $key='$invoiceNumber' and STA_0 = 3"
        val resultFromQuery = Client.sqlQuery(query)
        logger().info("invoiceData: $resultFromQuery")
        val records = ObjectMapper().readValue<MutableMap<String, Any?>>(resultFromQuery)
            .get("recordset") as ArrayList<String>
        if (records.size != 0) {
            val recordMap = records.toArray()[0] as HashMap<String, String>
            return recordMap["NUM_0"]
        }
        return null
    }

    open fun isPaymentPostedFromSage(paymentValue: String): String? {

        val query = "Select NUM_0 from $sageDatabase.PAYMENTH where UMRNUM_0='$paymentValue' AND STA_0 = 9"
        val resultFromQuery = Client.sqlQuery(query)
        logger().info("paymentData: $resultFromQuery")
        var records = ObjectMapper().readValue<MutableMap<String, Any?>>(resultFromQuery)
            .get("recordset") as ArrayList<String>
        if (records.size != 0) {
            val recordMap = records.toArray()[0] as HashMap<String, String>
            return recordMap["NUM_0"]
        }
        return null
    }

    private fun isJvPostedOnSage(jvNumber: String): String? {
        val query = "SELECT NUM_0 FROM $sageDatabase.GACCENTRY WHERE NUM_0 = '$jvNumber'"
        val resultFromQuery = Client.sqlQuery(query)
        logger().info("jvData: $resultFromQuery")
        var records = ObjectMapper().readValue<MutableMap<String, Any?>>(resultFromQuery)
            .get("recordset") as ArrayList<String>
        if (records.size != 0) {
            val recordMap = records.toArray()[0] as HashMap<String, String>
            return recordMap["NUM_0"]
        }
        return null
    }

    override suspend fun sagePaymentBySageRefNumbers(paymentNumValue: ArrayList<String?>): ArrayList<SagePaymentDetails> {
        val sqlQuery = """
            select P.NUM_0 as sage_payment_num, P.UMRNUM_0 as platform_payment_num, 
            case WHEN P.STA_0 = 9 THEN 'FINAL_POSTED'
                 WHEN P.STA_0 = 1 THEN 'POSTED' end as sage_status, 
                 P.FCY_0 as entity_code,
                 P.BPR_0 as bpr_number, 
                 P.ACC_0 as gl_code, 
                 P.CUR_0 as currency,
                 P.BPR_0 as sage_organization_id,
                 P.AMTCUR_0 as amount,
                 P.ACCDAT_0 as transaction_date,
                 P.BPANAM_0 as organization_name,
                 case when P.PAYTYP_0 in ('TDSC','TDS') and P.BPRSAC_0='AR' then 'CTDS' 
                  when P.PAYTYP_0 in ('TDSC','TDS') and P.BPRSAC_0='SC' then 'VTDS'
                  else P.PAYTYP_0 end as payment_code,
                 case when P.BPRSAC_0='SC' then 'AP' else 'AR' end as acc_mode,
                 case when P.BPRSAC_0='AR' then
                 (select XX1P4PANNO_0 from $sageDatabase.BPCUSTOMER where BPCNUM_0=P.BPR_0)
                 else (select XX1P4PANNO_0 from $sageDatabase.BPSUPPLIER where BPSNUM_0=P.BPR_0) end as pan_number,
                 P.DES_0 as narration
            from $sageDatabase.PAYMENTH P
            where UMRNUM_0 in ('${paymentNumValue.joinToString("','")}')
        """.trimIndent()
        val result = Client.sqlQuery(sqlQuery)
        val paymentRecords = ObjectMapper().readValue(result, PostPaymentInfos::class.java)
        return paymentRecords.recordSets!![0]
    }
}
