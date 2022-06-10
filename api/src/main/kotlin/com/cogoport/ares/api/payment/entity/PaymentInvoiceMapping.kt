package com.cogoport.ares.api.payment.entity

import com.cogoport.ares.model.payment.AccMode
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.math.BigDecimal
import java.sql.Timestamp

@MappedEntity(value = "payment_invoice_mapping")
data class PaymentInvoiceMapping (
        @field:Id @GeneratedValue var id: Long?,
        var accountMode:AccMode,
        var documentNo:Long,
        var paymentId:Long,
        var mappingType:String,
        var currency:String,
        var signFlag:Short,
        var amount:BigDecimal,
        var ledCurrency:String,
        var ledAmount:BigDecimal,
        var transactionDate:Timestamp,
        var createdAt: Timestamp? = Timestamp(System.currentTimeMillis()),
        var updatedAt: Timestamp? = Timestamp(System.currentTimeMillis())
 )
