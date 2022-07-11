package com.cogoport.ares.api.payment.mapper
import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.model.payment.AccountPayablesFile
import com.cogoport.ares.model.settlement.SettlementKnockoffRequest
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface PayableFileToPaymentMapper {

    @Mapping(source = "amount", target = "currencyAmount")
    @Mapping(source = "ledCurrency", target = "ledgerCurrency")
    @Mapping(source = "ledAmount", target = "ledgerAmount")
    @Mapping(source = "payMode", target = "paymentMode")
    fun convertToModel(payment: Payment): AccountPayablesFile

    @Mapping(source = "currencyAmount", target = "amount")
    @Mapping(source = "ledgerCurrency", target = "ledCurrency")
    @Mapping(source = "ledgerAmount", target = "ledAmount")
    @Mapping(source = "paymentMode", target = "payMode")
    fun convertToEntity(accPayFile: AccountPayablesFile): Payment

}
