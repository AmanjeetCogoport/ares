package com.cogoport.ares.payment.entity

import com.cogoport.ares.common.enums.AccMode
import com.cogoport.ares.payment.model.PayMode
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
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
    var signFlag: Int,
    var currency: String,
    var amount: BigDecimal,
    var ledCurrency: String,
    var ledAmount: BigDecimal,
    var payMode: PayMode?,
    var narration: String? = null,
    var transRefNumber: String?,
    var refPaymentId: Long?,
    var transactionDate: LocalDate?,
    var isPosted: Boolean,
    var isDeleted: Boolean,
    var createdAt: LocalDateTime?,
    var updatedAt: LocalDateTime,
    var accountNo:String
)
