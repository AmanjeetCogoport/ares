package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.model.payment.AccUtilizationRequest
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface AccountUtilizationMapper {

    @Mapping(source = "documentValue", target = "docValue")
    @Mapping(source = "amountCurr", target = "currencyAmount")
    @Mapping(source = "amountLoc", target = "ledgerAmount")
    @Mapping(source = "payCurr", target = "currencyPayment")
    @Mapping(source = "payLoc", target = "ledgerPayment")
    fun convertToModel(accountUtilization: AccountUtilization): AccUtilizationRequest

    @Mapping(source = "docValue", target = "documentValue")
    @Mapping(source = "currencyAmount", target = "amountCurr")
    @Mapping(source = "ledgerAmount", target = "amountLoc")
    @Mapping(source = "currencyPayment", target = "payCurr")
    @Mapping(source = "ledgerPayment", target = "payLoc")
    @Mapping(source = "docStatus", target = "documentStatus")
    fun convertToEntity(accountUtilizationRequest: AccUtilizationRequest): AccountUtilization
}
