package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.Settlement
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface SettlementMapper {

//    @BeforeMapping
//    fun beforeMapping(@MappingTarget target: SettledDocument, source: Settlement, accType: AccountType) = run {
//        when(accType){
//            AccountType.PCN -> {
//                target.documentNo = source.destinationId
//            }
//            AccountType.REC -> {
//                target.documentNo = source.sourceId
//            }
//        }
//    }

    @Mapping(source = "destinationId", target = "documentNo")
    @Mapping(target = "sid", expression = "java(null)")
    @Mapping(source = "ledAmount", target = "amountLedger")
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(source = "destinationType", target = "accType")
    @Mapping(source = "settlementDate", target = "transactionDate")
    @Mapping(target = "settlementStatus", ignore = true)
    fun convertDestinationToSettlementDocument(historyDocument: Settlement?): com.cogoport.ares.model.settlement.SettledDocument

    @Mapping(source = "sourceId", target = "documentNo")
    @Mapping(target = "sid", expression = "java(null)")
    @Mapping(source = "ledAmount", target = "amountLedger")
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(source = "sourceType", target = "accType")
    @Mapping(source = "settlementDate", target = "transactionDate")
    @Mapping(target = "settlementStatus", ignore = true)
    fun convertSourceToSettlementDocument(historyDocument: Settlement?): com.cogoport.ares.model.settlement.SettledDocument
}
