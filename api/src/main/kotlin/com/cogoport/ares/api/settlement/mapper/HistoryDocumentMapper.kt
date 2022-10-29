package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.HistoryDocument
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface HistoryDocumentMapper {

    @Mapping(source = "accType", target = "accountType")
    @Mapping(source = "amount", target = "documentAmount")
    @Mapping(source = "ledAmount", target = "ledgerAmount")
    @Mapping(source = "amount", target = "afterTdsAmount")
    @Mapping(source = "settledAmount", target = "allocationAmount")
    @Mapping(target = "balanceAfterAllocation", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "settledAllocation", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(source = "currentBalance", target = "balanceAmount")
    @Mapping(target = "settledTds", expression = "java(historyDocument.getSettledTds().subtract(historyDocument.getTds()))")
    fun convertToModel(historyDocument: HistoryDocument?): com.cogoport.ares.model.settlement.HistoryDocument
}
