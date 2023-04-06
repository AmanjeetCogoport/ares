package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface AccUtilizationToPaymentMapper {

    @Mapping(source = "amount", target = "currencyAmount")
    @Mapping(target = "isVoid", expression = "java(false)")
    fun convertEntityToModel(
        accUtilization: com.cogoport.ares.api.payment.entity.Payment
    ): AccUtilizationRequest

    @Mapping(source = "currencyAmount", target = "amountCurr")
    @Mapping(source = "ledgerAmount", target = "amountLoc")
    @Mapping(source = "currencyPayment", target = "payCurr")
    @Mapping(source = "ledgerPayment", target = "payLoc")
    @Mapping(source = "docStatus", target = "documentStatus")
    fun convertModelToEntity(accUtilization: AccUtilizationRequest): AccountUtilization
}
