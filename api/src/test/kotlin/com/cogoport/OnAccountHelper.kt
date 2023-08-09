package com.cogoport

import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.PaymentDocumentStatus
import com.cogoport.ares.model.payment.response.PaymentResponse
import com.cogoport.plutus.model.invoice.GetUserResponse
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.util.UUID

@Singleton
class OnAccountHelper () {
    fun getUserResponse ():List<GetUserResponse> {
        return listOf(
            GetUserResponse(
                userEmail = "vivek.garg@cogoport.com",
                userId = UUID.fromString("ec306da2-0d52-4cc1-a7b1-d3a6541f1ce8"),
                userName = "vivekgarg"
            )
        )
    }

    fun getAccountReceivables (payment: Payment) :  List<PaymentResponse?> {
        return listOf(
            PaymentResponse(
                id = payment.id,
                orgSerialId = 122334,
                bankAccountNumber = "123456789876543",
                entityCode = 301,
                bankName = "rbl",
                accCode = 321000,
                sageOrganizationId = null,
                currency = "INR",
                amount = BigDecimal.valueOf(100.0).setScale(4),
                ledCurrency = "INR",
                ledAmount = BigDecimal.valueOf(100.0).setScale(4),
                updatedBy = null,
                utr = "abhishek_boss",
                accMode = AccMode.AP,
                paymentCode = PaymentCode.PAY,
                paymentDocumentStatus = PaymentDocumentStatus.APPROVED,
                transactionDate = payment.transactionDate,
                uploadedBy = null,
                exchangeRate = BigDecimal.valueOf(1.0).setScale(6),
                paymentNum = 12345,
                paymentNumValue = "PAY12345",
                createdAt = payment.createdAt,
                deletedAt = null,
                narration = null,
                bankId = UUID.fromString("d646dc1c-f366-453c-b56f-2788f36c4136"),
                payMode = PayMode.BANK,
                createdBy = UUID.fromString("ec306da2-0d52-4cc1-a7b1-d3a6541f1ce8"),
                organizationName = "Inext Logistics & Supply Chain Private Limited",
                organizationId = UUID.fromString("9f03db0c-88cc-450f-bbb1-38fa31861911")
            )
        )
    }
}