package com.cogoport.ares.api.migration.mapper

import com.cogoport.ares.api.migration.entity.PaymentMigrationEntity
import com.cogoport.ares.api.migration.model.PaymentMigration
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
    fun convertToEntity(payment: PaymentMigration): PaymentMigrationEntity

    @Mapping(source = "amount", target = "currencyAmount")
    fun convertPaymentToAccUtilizationRequest(
        accUtilization: PaymentMigrationEntity
    ): AccUtilizationRequest
}
