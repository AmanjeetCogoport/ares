package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.PayMode
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.UUID

@MappedEntity(value = "payments")
data class Payment(
    @field:Id @GeneratedValue var id: Long?,
    var entityCode: Int,
    var fileId: Long? = null,
    var orgSerialId: Long,
    var organizationId: UUID,
    var organizationName: String,
    var sageOrganizationId: String?,
    var accCode: Int,
    var accMode: AccMode,
    var signFlag: Short,
    var currency: String,
    var amount: BigDecimal,
    var ledCurrency: String,
    var ledAmount: BigDecimal,
    var payMode: PayMode?,
    var narration: String? = null,
    var transRefNumber: String?,
    var refPaymentId: Long?,
    var transactionDate: Timestamp?,
    var isPosted: Boolean,
    var isDeleted: Boolean,
    var createdAt: Timestamp?,
    var updatedAt: Timestamp?,
    var accountNo: String?
)
