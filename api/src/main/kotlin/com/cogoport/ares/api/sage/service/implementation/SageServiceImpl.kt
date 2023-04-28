package com.cogoport.ares.api.sage.service.implementation

import com.cogoport.ares.api.sage.service.interfaces.SageService
import com.cogoport.ares.model.payment.AccountType
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
        // Check if PINV, PCN exists and it is posted in sage
        if (arrayListOf(AccountType.PINV, AccountType.PCN, AccountType.PDN).contains(documentType)) {
            return isBillDataPostedFromSage(documentValue, organizationSerialId, sageBPRNumber, registrationNumber)
        }

        // Check if REC, PAY exists and it is posted in sage
        if (arrayListOf(AccountType.REC, AccountType.PAY).contains(documentType)) {
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
        val records = ObjectMapper().readValue<MutableMap<String, Any?>>(resultFromQuery)
            .get("recordset") as ArrayList<String>
        if (records.size != 0) {
            val recordMap = records.toArray()[0] as HashMap<String, String>
            return recordMap["NUM_0"]
        }
        return null
    }

    open fun isPaymentPostedFromSage(paymentValue: String): String? {

        val query = "Select NUM_0 from $sageDatabase.PAYMENTH where UMRNUM_0='$paymentValue'"
        val resultFromQuery = Client.sqlQuery(query)
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
        var records = ObjectMapper().readValue<MutableMap<String, Any?>>(resultFromQuery)
            .get("recordset") as ArrayList<String>
        if (records.size != 0) {
            val recordMap = records.toArray()[0] as HashMap<String, String>
            return recordMap["NUM_0"]
        }
        return null
    }
}
