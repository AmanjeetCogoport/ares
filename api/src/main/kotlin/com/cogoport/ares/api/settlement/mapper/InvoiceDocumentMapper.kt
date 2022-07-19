package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.InvoiceDocument
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface InvoiceDocumentMapper {
    @Mapping(target = "settledAllocation", expression = "java(null)")
    @Mapping(source = "documentLedAmount", target = "ledgerAmount")
    @Mapping(source = "documentDate", target = "transactionDate")
    fun convertToModel(document: InvoiceDocument): com.cogoport.ares.model.settlement.Document
}
