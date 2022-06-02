package com.cogoport.ares.api.payment.mapper

import com.cogoport.ares.api.payment.entity.AccountUtilization
import com.cogoport.ares.api.payment.model.AccountPayablesFile
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface PayableFileToAccountUtilMapper {

    @Mapping(source = "amountCurr", target = "currencyAmount")
    @Mapping(source = "ledCurrency", target = "ledgerCurrency")
    @Mapping(source = "amountLoc", target = "ledgerAmount")
    fun convertToModel(accUtils: AccountUtilization): AccountPayablesFile

    @Mapping(source = "currencyAmount", target = "amountCurr")
    @Mapping(source = "ledgerCurrency", target = "ledCurrency")
    @Mapping(source = "ledgerAmount", target = "amountLoc")
    fun convertToEntity(accPayFile: AccountPayablesFile): AccountUtilization
}
