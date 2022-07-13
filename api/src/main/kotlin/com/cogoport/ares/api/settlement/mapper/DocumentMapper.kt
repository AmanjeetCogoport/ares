package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.Document
import com.cogoport.ares.model.settlement.Invoice
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface DocumentMapper {

    fun convertToInvoice(doc: List<com.cogoport.ares.model.settlement.Document?>): List<Invoice?>

    @Mapping(source = "documentNo", target = "invoiceNo")
    @Mapping(source = "documentValue", target = "invoiceValue")
    @Mapping(source = "transactionDate", target = "invoiceDate")
    @Mapping(source = "documentAmount", target = "invoiceAmount")
    fun documentToInvoice(doc: com.cogoport.ares.model.settlement.Document?): Invoice?

    @Mapping(target = "settledAllocation", expression = "java(null)")
    @Mapping(source = "documentLedAmount", target = "ledgerAmount")
    @Mapping(source = "documentDate", target = "transactionDate")
    fun convertToModel(document: Document): com.cogoport.ares.model.settlement.Document
}
