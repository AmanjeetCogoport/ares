package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.Payment
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface PaymentToPaymentMapper {
    @Mapping(source = "entityType", target = "entityCode")
    @Mapping(source = "remarks", target = "narration")
    @Mapping(source = "utr", target = "transRefNumber")
    @Mapping(source = "bankAccountNumber", target = "cogoAccountNo")
    fun convertToEntity(payment: com.cogoport.ares.model.payment.Payment): Payment

    @Mapping(source = "entityCode", target = "entityType")
    @Mapping(source = "narration", target = "remarks")
    @Mapping(source = "transRefNumber", target = "utr")
    @Mapping(source = "cogoAccountNo", target = "bankAccountNumber")
    fun convertToModel(payment: Payment): com.cogoport.ares.model.payment.Payment
}
