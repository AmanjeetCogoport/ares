package com.cogoport.ares.payment.mapper

import com.cogoport.ares.payment.entity.Payment
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface PaymentToPaymentMapper {

    @Mapping(source = "organizationId", target = "customerId")
    fun convertToModel(payment: Payment): com.cogoport.ares.payment.model.Payment

    @Mapping(source = "customerId", target = "organizationId")
    fun convertToEntity(payment: com.cogoport.ares.payment.model.Payment): Payment
}
