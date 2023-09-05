package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.common.models.ARLedgerJobDetailsResponse
import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.model.payment.request.AccUtilizationRequest
import com.cogoport.ares.model.payment.response.ARLedgerResponse
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface AccountUtilizationMapper {

    @Mapping(source = "documentValue", target = "docValue")
    @Mapping(source = "amountCurr", target = "currencyAmount")
    @Mapping(source = "amountLoc", target = "ledgerAmount")
    @Mapping(source = "payCurr", target = "currencyPayment")
    @Mapping(source = "payLoc", target = "ledgerPayment")
    @Mapping(target = "isVoid", expression = "java(false)")
    fun convertToModel(accountUtilization: AccountUtilization?): AccUtilizationRequest

    @Mapping(source = "docValue", target = "documentValue")
    @Mapping(source = "currencyAmount", target = "amountCurr")
    @Mapping(source = "ledgerAmount", target = "amountLoc")
    @Mapping(source = "currencyPayment", target = "payCurr")
    @Mapping(source = "ledgerPayment", target = "payLoc")
    @Mapping(source = "docStatus", target = "documentStatus")
    @Mapping(target = "isVoid", expression = "java(false)")
    fun convertToEntity(accountUtilizationRequest: AccUtilizationRequest): AccountUtilization

    @Mapping(target = "creditBalance", constant = "0")
    @Mapping(target = "debitBalance", constant = "0")
    fun convertARLedgerJobDetailsResponseToARLedgerResponse(arLedgerJobDetailsResponse: ARLedgerJobDetailsResponse): ARLedgerResponse
    fun convertARLedgerJobDetailsResponseToARLedgerResponse(arLedgerJobDetailsResponse: List<ARLedgerJobDetailsResponse>): List<ARLedgerResponse>
}
