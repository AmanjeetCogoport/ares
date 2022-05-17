package com.cogoport.ares.payment.mapper

import com.cogoport.ares.payment.entity.Payment
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface PaymentToPaymentMapper {

    @Mapping(source = "organizationId", target = "customerId")
    @Mapping(source = "organizationName", target = "customerName")
    @Mapping(source = "entityCode", target = "entityType")
    @Mapping(source = "currency", target = "currencyType")
    @Mapping(source = "transRefNumber", target = "utr")
    @Mapping(source = "narration", target = "remarks")
    fun convertToModel(payment: Payment): com.cogoport.ares.payment.model.Payment

    @Mapping(source = "customerId", target = "organizationId")
    @Mapping(source = "customerName", target = "organizationName")
    @Mapping(source = "entityType", target = "entityCode")
    @Mapping(source = "currencyType", target = "currency")
    @Mapping(source = "utr", target = "transRefNumber")
    @Mapping(source = "remarks", target = "narration")
    fun convertToEntity(payment: com.cogoport.ares.payment.model.Payment): Payment
}
