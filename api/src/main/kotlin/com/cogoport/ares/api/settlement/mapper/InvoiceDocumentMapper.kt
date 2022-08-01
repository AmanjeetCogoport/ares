package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.InvoiceDocument
import com.cogoport.ares.model.settlement.InvoiceDocumentResponse
import org.mapstruct.Mapper

@Mapper
interface InvoiceDocumentMapper {

    fun convertToModel(document: InvoiceDocument): InvoiceDocumentResponse
}
