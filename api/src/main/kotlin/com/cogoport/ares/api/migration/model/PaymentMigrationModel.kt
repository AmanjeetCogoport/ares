package com.cogoport.ares.api.migration.model

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.PayMode
import com.cogoport.ares.model.payment.PaymentCode
import com.cogoport.ares.model.payment.ServiceType
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.core.annotation.Introspected
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@Introspected
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PaymentMigrationModel(
    var id: Long? = null,
    var entityCode: Int,
    var orgSerialId: Long? = null,
    var sageOrganizationId: String,
    var organizationId: UUID? = null,
    var organizationName: String? = null,
    var accCode: Int,
    var accMode: AccMode,
    var signFlag: Short,
    var currency: String,
    var amount: BigDecimal,
    var ledCurrency: String,
    var ledAmount: BigDecimal,
    var payMode: PayMode,
    var narration: String? = null,
    var cogoAccountNo: String? = null,
    var refAccountNo: String? = null,
    var bankName: String? = null,
    var transRefNumber: String? = null,
    var refPaymentId: Long? = null,
    var transactionDate: Timestamp? = null,
    var isPosted: Boolean,
    var isDeleted: Boolean,
    var createdAt: Timestamp,
    var updatedAt: Timestamp,
    var paymentCode: PaymentCode,
    var paymentNum: Long,
    var paymentNumValue: String,
    var exchangeRate: BigDecimal,
    var bankId: UUID? = null,
    var tradePartyMappingId: UUID? = null,
    var taggedOrganizationId: UUID? = null,
    var bankPayAmount: BigDecimal? = null,

    var zone: String? = null,
    var serviceType: ServiceType? = null,
    var accountUtilCurrAmount: BigDecimal,
    var accountUtilLedAmount: BigDecimal,
    var accountUtilPayCurr: BigDecimal,
    var accountUtilPayLed: BigDecimal,
    var accountType: AccountType,

)
