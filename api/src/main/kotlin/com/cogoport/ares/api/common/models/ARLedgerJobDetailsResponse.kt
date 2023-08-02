package com.cogoport.ares.api.common.models

import com.cogoport.loki.model.job.DocumentDetail
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import java.math.BigDecimal

@Introspected
@MappedEntity
data class ARLedgerJobDetailsResponse(
    @JsonProperty("Transaction Date")
    val transactionDate: String?,
    @JsonProperty("Document Type")
    val documentType: String?,
    @JsonProperty("Document Number")
    val documentNumber: String,
    @JsonProperty("Original Transaction Currency")
    val currency: String?,
    @JsonProperty("Original Transaction Amount")
    val amount: String?,
    @JsonProperty("Ledger Debit")
    val debit: BigDecimal,
    @JsonProperty("Ledger Credit")
    val credit: BigDecimal,
    @JsonProperty("Transaction Ref Number")
    val transactionRefNumber: String?,
    @JsonProperty("Job Documents")
    @MappedProperty(type = DataType.JSON)
    val jobDocuments: MutableList<DocumentDetail>?,
    @JsonProperty("shipmentDocumentNumber")
    var shipmentDocumentNumber: String?
)
