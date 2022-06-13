package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.Payment
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface PaymentToPaymentMapper {
    @Mapping(source = "entityType", target = "entityCode")
    @Mapping(source = "customerName", target = "organizationName")
    @Mapping(source = "currencyType", target = "currency")
    @Mapping(source = "remarks", target = "narration")
    @Mapping(source = "utr", target = "transRefNumber")
    @Mapping(source = "bankAccountNumber", target = "cogoAccountNo")
    fun convertToEntity(payment: com.cogoport.ares.model.payment.Payment): Payment

    @Mapping(source = "entityCode", target = "entityType")
    @Mapping(source = "organizationName", target = "customerName")
    @Mapping(source = "currency", target = "currencyType")
    @Mapping(source = "narration", target = "remarks")
    @Mapping(source = "transRefNumber", target = "utr")
    @Mapping(source = "cogoAccountNo", target = "bankAccountNumber")
    fun convertToModel(payment: Payment): com.cogoport.ares.model.payment.Payment
}
