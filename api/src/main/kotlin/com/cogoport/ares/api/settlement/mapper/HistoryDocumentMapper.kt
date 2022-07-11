package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.HistoryDocument
import org.mapstruct.Mapper

@Mapper
interface HistoryDocumentMapper {

    fun convertToModel(historyDocument: HistoryDocument?): com.cogoport.ares.model.settlement.HistoryDocument

    fun convertToEntity(historyDocument: com.cogoport.ares.model.settlement.HistoryDocument): HistoryDocument
}
