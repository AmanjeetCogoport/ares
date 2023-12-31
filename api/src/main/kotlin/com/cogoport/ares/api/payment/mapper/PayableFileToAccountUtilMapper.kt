package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.model.payment.AccountPayablesFile
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface PayableFileToAccountUtilMapper {

    @Mapping(source = "amountCurr", target = "currencyAmount")
    @Mapping(source = "ledCurrency", target = "ledgerCurrency")
    @Mapping(source = "amountLoc", target = "ledgerAmount")
    fun convertToModel(accUtils: AccountUtilization): AccountPayablesFile

    @Mapping(source = "currencyAmount", target = "amountCurr")
    @Mapping(source = "ledgerCurrency", target = "ledCurrency")
    @Mapping(source = "ledgerAmount", target = "amountLoc")
    @Mapping(target = "isVoid", expression = "java(false)")
    fun convertToEntity(accPayFile: AccountPayablesFile): AccountUtilization
}
