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

    override suspend fun checkIfDocumentExistInSage(documentValue: String, sageBPRNumber: String, organizationSerialId: Long?, documentType: AccountType, registrationNumber: String?): Boolean {
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

        // Others are JV check if it is present in sage
        return false
    }

    private fun isBillDataPostedFromSage(billNumber: String?, organizationSerialId: Long?, sageOrganizationId: String?, registrationNumber: String?): Boolean {
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

        return records.size != 0
    }

    private fun isInvoiceDataPostedFromSage(key: String, sageDatabase: String?, invoiceNumber: String?): Boolean {
        val query = "Select $key from $sageDatabase where $key='$invoiceNumber' and STA_0 = 3"
        val resultFromQuery = Client.sqlQuery(query)
        val records = ObjectMapper().readValue<MutableMap<String, Any?>>(resultFromQuery)
            .get("recordset") as ArrayList<String>

        return records.size != 0
    }

    private fun isPaymentPostedFromSage(paymentValue: String): Boolean {
        val query = "Select UMRNUM_0 from $sageDatabase.PAYMENTH where UMRNUM_0='$paymentValue'"
        val resultFromQuery = Client.sqlQuery(query)
        val records = ObjectMapper().readValue<MutableMap<String, Any?>>(resultFromQuery)
            .get("recordset") as ArrayList<String>

        return records.size != 0
    }
}
