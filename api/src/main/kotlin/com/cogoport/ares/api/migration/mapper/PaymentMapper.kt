package com.cogoport.ares.api.migration.mapper

import com.cogoport.ares.api.migration.entity.PaymentMigration
import com.cogoport.ares.model.payment.AccUtilizationRequest
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface PaymentMapper {

    @Mapping(source = "entityType", target = "entityCode")
    @Mapping(source = "remarks", target = "narration")
    @Mapping(source = "utr", target = "transRefNumber")
    @Mapping(source = "bankAccountNumber", target = "cogoAccountNo")
    fun convertToEntity(payment: com.cogoport.ares.model.payment.Payment): PaymentMigration

    @Mapping(source = "amount", target = "currencyAmount")
    fun convertPaymentToAccUtilizationRequest(
        accUtilization: PaymentMigration
    ): AccUtilizationRequest
}
