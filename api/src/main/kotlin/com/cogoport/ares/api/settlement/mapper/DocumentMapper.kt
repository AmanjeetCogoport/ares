package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.Document
import com.cogoport.ares.model.settlement.Invoice
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface DocumentMapper {
    @Mapping(source = "documentNo", target = "invoiceNo")
    @Mapping(source = "documentDate", target = "invoiceDate")
    fun convertToInvoice(doc: List<com.cogoport.ares.model.settlement.Document?>): List<Invoice?>

    fun convertToModel(document: Document): com.cogoport.ares.model.settlement.Document
}
