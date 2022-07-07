package com.cogoport.ares.api.settlement.mapper


import com.cogoport.ares.api.settlement.entity.Settlement
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface SettlementMapper {

    @Mapping(source = "amountCurr", target = "currencyAmount")
    fun convertToSettlementDocument(historyDocument: Settlement?): com.cogoport.ares.model.settlement.SettledDocument

}