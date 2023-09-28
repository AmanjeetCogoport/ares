
package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.Document
import com.cogoport.ares.model.payment.response.LSPLedgerDocuments
import com.cogoport.ares.model.payment.response.LedgerDetails
import com.cogoport.ares.model.settlement.CheckDocument
import com.cogoport.ares.model.settlement.Invoice
import com.cogoport.ares.model.settlement.InvoiceDocumentResponse
import com.cogoport.ares.model.settlement.SettlementInvoiceResponse
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface DocumentMapper {

    fun convertToInvoice(doc: List<com.cogoport.ares.model.settlement.Document?>): List<Invoice?>
    fun convertToSettlementInvoice(doc: List<InvoiceDocumentResponse?>): List<SettlementInvoiceResponse?>

    @Mapping(source = "documentNo", target = "invoiceNo")
    @Mapping(source = "documentValue", target = "invoiceValue")
    @Mapping(source = "transactionDate", target = "invoiceDate")
    @Mapping(source = "documentAmount", target = "invoiceAmount")
    fun documentToInvoice(doc: com.cogoport.ares.model.settlement.Document?): Invoice?

    @Mapping(source = "documentNo", target = "invoiceNo")
    @Mapping(source = "documentValue", target = "invoiceValue")
    @Mapping(source = "documentDate", target = "invoiceDate")
    @Mapping(source = "documentAmount", target = "invoiceAmount")
    fun documentToSettlementInvoiceResponse(doc: InvoiceDocumentResponse?): SettlementInvoiceResponse?

    @Mapping(target = "settledAllocation", expression = "java(null)")
    @Mapping(source = "documentLedAmount", target = "ledgerAmount")
    @Mapping(source = "documentDate", target = "transactionDate")
    @Mapping(source = "balanceAmount", target = "currentBalance")
    @Mapping(source = "documentLedBalance", target = "ledgerBalance")
    @Mapping(source = "tds", target = "tds")
    @Mapping(target = "nostroAmount", expression = "java(java.math.BigDecimal.ZERO)")
    fun convertToModel(document: Document): com.cogoport.ares.model.settlement.Document

    //    fun convertToModel(document: Document): com.cogoport.ares.model.settlement.Document

    fun convertToIncidentModel(document: CheckDocument): com.cogoport.hades.model.incident.request.CheckDocument

    fun convertLedgerDetailsToLSPLedgerDocuments(request: List<LedgerDetails>): MutableList<LSPLedgerDocuments>

    @Mapping(target = "settledNostro", expression = "java(java.math.BigDecimal.ZERO)")
    fun convertDocumentModelToCheckDocument(request: com.cogoport.ares.model.settlement.Document): CheckDocument
}
