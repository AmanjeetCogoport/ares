package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.payment.entity.Payment
import com.cogoport.ares.api.settlement.entity.SettledInvoice
import com.cogoport.ares.model.settlement.SettlementKnockoffRequest
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface SettledInvoiceMapper {
    fun convertToModel(historyDocument: SettledInvoice?): com.cogoport.ares.model.settlement.SettledInvoice

    @Mapping(source = "currencyAmount", target = "amount")
    @Mapping(source = "ledgerCurrency", target = "ledCurrency")
    @Mapping(source = "ledgerAmount", target = "ledAmount")
    @Mapping(source = "paymentMode", target = "payMode")
    fun convertKnockoffRequestToEntity(request: SettlementKnockoffRequest): Payment
}
