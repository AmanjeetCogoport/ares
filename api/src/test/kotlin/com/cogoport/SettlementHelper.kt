package com.cogoport

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.repository.AccountUtilizationRepo
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocumentStatus
import com.cogoport.ares.model.payment.ServiceType
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@Singleton
class SettlementHelper (
    private var accountUtilizationRepo: AccountUtilizationRepo,
) {
    suspend fun saveAccountUtilizations(): Long? {
        var accUtilDoc = getAccUtilDoc(
            AccMode.AP,
            AccountType.PINV,
            321000,
            "VIVEK/12234",
            12345,
            -1,
            DocumentStatus.FINAL,
            301,
            BigDecimal(40),
            BigDecimal(40),
            "INR",
            "INR",
            BigDecimal(20),
            BigDecimal(20),
            BigDecimal(100),
            BigDecimal(100),
        )

        accUtilDoc = accountUtilizationRepo.save(accUtilDoc)

        return accUtilDoc.id
    }

    private fun getAccUtilDoc(
        accMode: AccMode,
        accType: AccountType,
        accCode: Int,
        documentValue: String,
        documentNo: Long,
        signFlag: Short,
        documentStatus: DocumentStatus,
        entityCode: Int,
        payCurr: BigDecimal,
        payLoc: BigDecimal,
        currency: String,
        ledCurrency: String,
        tdsAmount: BigDecimal?,
        tdsAmountLoc: BigDecimal?,
        amountCurr: BigDecimal,
        amountLoc: BigDecimal,
    ): AccountUtilization {
        return AccountUtilization(
            id = null,
            documentNo = documentNo,
            documentValue = documentValue,
            accMode =  accMode,
            accCode = accCode,
            accType = accType,
            signFlag = signFlag,
            amountCurr = amountCurr,
            amountLoc = amountLoc,
            category = "ASSET",
            documentStatus = documentStatus,
            dueDate = Date(),
            entityCode =entityCode,
            transactionDate = Date(),
            migrated = false,
            organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
            organizationName = "my_company",
            orgSerialId = 122122,
            sageOrganizationId = "122122",
            serviceType = ServiceType.FCL_FREIGHT.name,
            taggedOrganizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911"),
            tradePartyMappingId = null,
            zoneCode = "NORTH",
            payCurr = payCurr,
            payLoc = payLoc,
            currency = currency,
            ledCurrency = ledCurrency,
            tdsAmount = tdsAmount,
            tdsAmountLoc = tdsAmountLoc
        )
    }
}