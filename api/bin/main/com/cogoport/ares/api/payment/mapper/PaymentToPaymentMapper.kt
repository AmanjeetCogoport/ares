package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.Payment
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface PaymentToPaymentMapper {

    @Mapping(source = "organizationId", target = "customerId")
    fun convertToModel(payment: Payment): com.cogoport.ares.model.payment.Payment

    @Mapping(source = "customerId", target = "organizationId")
    fun convertToEntity(payment: com.cogoport.ares.model.payment.Payment): Payment
}
